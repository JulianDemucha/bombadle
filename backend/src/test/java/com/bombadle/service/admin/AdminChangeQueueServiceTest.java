package com.bombadle.service.admin;

import com.bombadle.dto.AdminPendingCardChangeDto;
import com.bombadle.dto.queue.PendingCacheFlushPayload;
import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.entity.AdminPendingChange;
import com.bombadle.repository.AdminPendingChangeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminChangeQueueServiceTest {

    @InjectMocks
    private AdminChangeQueueService adminChangeQueueService;

    @Mock
    private AdminPendingChangeRepository repo;

    @Mock
    private AdminCharacterCardProcessor processor;

    @Mock
    private CharacterCardImageService imageService;

    @Mock
    private ObjectMapper objectMapper;

    @Nested
    class EnqueueTests {

        @Test
        void enqueue_actionKeyIsNull_skipsDeduplicationAndSavesNewTask() throws JsonProcessingException {
            // Arrange
            Object payload = new Object();
            when(objectMapper.writeValueAsString(payload)).thenReturn("some payload");

            // Act
            adminChangeQueueService.enqueue("sigma_action_type", payload);

            // Assert
            verify(repo, never()).findFirstByActionKey(null);
            ArgumentCaptor<AdminPendingChange> captor = ArgumentCaptor.forClass(AdminPendingChange.class);

            verify(repo).save(captor.capture());
            AdminPendingChange change = captor.getValue();
            assertEquals("sigma_action_type", change.getActionType());
            assertNull(change.getActionKey());
            assertEquals("some payload", change.getPayload());
        }

        @Test
        void enqueue_taskWithSameActionKeyExists_overwritesExistingTask() throws JsonProcessingException {
            // Arrange
            Object payload = new Object();
            when(objectMapper.writeValueAsString(payload)).thenReturn("some payload");
            when(repo.findFirstByActionKey("beta_action_key")).thenReturn(Optional.of(
                    AdminPendingChange
                            .builder()
                            .actionKey("beta_action_key")
                            .actionType("not_sigma_action_type")
                            .build()
            ));

            // Act
            adminChangeQueueService.enqueue("sigma_action_type", "beta_action_key", payload);

            // Assert
            ArgumentCaptor<AdminPendingChange> captor = ArgumentCaptor.forClass(AdminPendingChange.class);
            verify(repo).save(captor.capture());
            AdminPendingChange change = captor.getValue();
            assertEquals("sigma_action_type", change.getActionType());
            assertEquals("beta_action_key", change.getActionKey());
            assertEquals("some payload", change.getPayload());
        }

        @Test
        void enqueue_validActionKeyAndTaskDoesNotExist_overwritesExistingTask() throws JsonProcessingException {
            // Arrange
            Object payload = new Object();
            when(objectMapper.writeValueAsString(payload)).thenReturn("some payload");
            when(repo.findFirstByActionKey("beta_action_key")).thenReturn(Optional.empty());

            // Act
            adminChangeQueueService.enqueue("sigma_action_type", "beta_action_key", payload);

            // Assert
            ArgumentCaptor<AdminPendingChange> captor = ArgumentCaptor.forClass(AdminPendingChange.class);
            verify(repo).save(captor.capture());
            AdminPendingChange change = captor.getValue();
            assertEquals("sigma_action_type", change.getActionType());
            assertEquals("beta_action_key", change.getActionKey());
            assertEquals("some payload", change.getPayload());
        }

        @Test
        void enqueue_payloadIsNull_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    adminChangeQueueService.enqueue("sigma_action_type", null)
            );
        }

        @Test
        void enqueue_serializationFails_throwsIllegalStateException() throws JsonProcessingException {
            // Arrange
            Object payload = new Object();
            when(objectMapper.writeValueAsString(payload)).thenThrow(new JsonProcessingException("not sigma error") {});

            // Act & Assert
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                    adminChangeQueueService.enqueue("sigma_action_type", "some_key", payload)
            );

            assertEquals("Failed to serialize pending change payload", exception.getMessage());
        }
    }

    @Nested
    class ApplyAllTests {

        @Test
        void applyAll_noChanges_doNothing() {
            // Arrange
            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of());

            // Act
            adminChangeQueueService.applyAll();

            // Assert
            verify(repo, never()).delete(any());
            verify(processor, never()).processDelete(any());
        }

        @Test
        void applyAll_validChange_processesSuccessfullyAndDeletesTask() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder()
                    .id(1L)
                    .actionType("create_card")
                    .payload("valid_json")
                    .build();
            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change));

            PendingCardCreatePayload expectedPayload = new PendingCardCreatePayload(null, null);
            when(objectMapper.readValue("valid_json", PendingCardCreatePayload.class))
                    .thenReturn(expectedPayload);

            // Act
            adminChangeQueueService.applyAll();

            // Assert
            verify(processor).processCreate(expectedPayload);
            verify(repo).delete(change);
        }

        @Test
        void applyAll_changeThrowsException_catchesExceptionAndStillDeletesTask() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder()
                    .id(2L)
                    .actionType("delete_card")
                    .payload("bad_json")
                    .build();
            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change));

            when(objectMapper.readValue("bad_json", PendingCardDeletePayload.class))
                    .thenThrow(new RuntimeException("Mocked parsing error"));

            // Act
            adminChangeQueueService.applyAll();

            // Assert
            verify(processor, never()).processDelete(any());
            verify(repo).delete(change);
        }

        @Test
        void applyAll_multipleChangesWithOneFailure_processesOthersAndDeletesAll() throws Exception {
            // Arrange
            AdminPendingChange badChange = AdminPendingChange.builder()
                    .id(1L).actionType("create_card").payload("bad_json").build();
            AdminPendingChange goodChange = AdminPendingChange.builder()
                    .id(2L).actionType("flush_cache").payload("good_json").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(badChange, goodChange));

            when(objectMapper.readValue("bad_json", PendingCardCreatePayload.class))
                    .thenThrow(new RuntimeException("Parse error"));

            PendingCacheFlushPayload cachePayload = new PendingCacheFlushPayload("all", true);
            when(objectMapper.readValue("good_json", PendingCacheFlushPayload.class))
                    .thenReturn(cachePayload);

            // Act
            adminChangeQueueService.applyAll();

            // Assert
            verify(processor).processCacheFlush(cachePayload);
            verify(repo).delete(badChange);
            verify(repo).delete(goodChange);
        }
    }

    @Nested
    class StateCheckingTests {

        @Test
        void hasPendingActionKey_actionKeyIsNull_returnsFalse() {
            boolean result = adminChangeQueueService.hasPendingActionKey(null);
            verify(repo, never()).findFirstByActionKey(null);
            assertFalse(result);
        }

        @Test
        void hasPendingActionKey_actionKeyIsBlank_returnsFalse() {
            boolean result = adminChangeQueueService.hasPendingActionKey(" ");
            verify(repo, never()).findFirstByActionKey(" ");
            assertFalse(result);
        }

        @Test
        void hasPendingActionKey_actionKeyDoesNotExist_returnsFalse() {
            when(repo.findFirstByActionKey("some_key")).thenReturn(Optional.empty());
            boolean result = adminChangeQueueService.hasPendingActionKey("some_key");
            assertFalse(result);
        }

        @Test
        void hasPendingActionKey_actionKeyExists_returnsTrue() {
            when(repo.findFirstByActionKey("some_key")).thenReturn(Optional.of(AdminPendingChange.builder().actionKey("some_key").build()));
            boolean result = adminChangeQueueService.hasPendingActionKey("some_key");
            assertTrue(result);
        }

        @Test
        void hasPendingCardName_nameIsNull_returnsFalse() {
            assertFalse(adminChangeQueueService.hasPendingCardName(null, 1L));
        }

        @Test
        void hasPendingCardName_createCardWithMatchingSlugExists_returnsTrue() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder().actionType("create_card").payload("{\"card\":{\"name\":\"Test Card\"}}").build();
            when(repo.findAll()).thenReturn(List.of(change));
            when(imageService.buildSlug("Test Card")).thenReturn("test-card");

            AdminCharacterCardRequest mockCard = mock(AdminCharacterCardRequest.class);
            when(mockCard.name()).thenReturn("Test Card");

            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class)))
                    .thenReturn(new PendingCardCreatePayload(mockCard, null));

            // Act & Assert
            assertTrue(adminChangeQueueService.hasPendingCardName("Test Card", null));
        }

        @Test
        void hasPendingCardName_updateCardWithMatchingSlugExists_returnsTrue() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder().actionType("update_card").payload("{\"id\":1, \"card\":{\"name\":\"Updated Card\"}}").build();
            when(repo.findAll()).thenReturn(List.of(change));
            when(imageService.buildSlug("Updated Card")).thenReturn("updated-card");

            AdminCharacterCardRequest mockCard = mock(AdminCharacterCardRequest.class);
            when(mockCard.name()).thenReturn("Updated Card");

            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class)))
                    .thenReturn(new PendingCardUpdatePayload(1L, mockCard, null, null));

            // Act & Assert
            assertTrue(adminChangeQueueService.hasPendingCardName("Updated Card", 2L));
        }

        @Test
        void hasPendingCardName_updateCardMatchesButIsExcluded_returnsFalse() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder().actionType("update_card").payload("{\"id\":1, \"card\":{\"name\":\"Updated Card\"}}").build();
            when(repo.findAll()).thenReturn(List.of(change));

            AdminCharacterCardRequest mockCard = mock(AdminCharacterCardRequest.class);
            when(mockCard.name()).thenReturn("Updated Card");

            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class)))
                    .thenReturn(new PendingCardUpdatePayload(1L, mockCard, null, null));

            // Act & Assert
            assertFalse(adminChangeQueueService.hasPendingCardName("Updated Card", 1L));
        }
    }

    @Nested
    class CancelTests {

        @Test
        void cancelByActionKey_keyIsNull_returnsFalse() {
            assertFalse(adminChangeQueueService.cancelByActionKey(null));
            verify(repo, never()).delete(any());
        }

        @Test
        void cancelByActionKey_keyDoesNotExist_returnsFalse() {
            when(repo.findFirstByActionKey("key")).thenReturn(Optional.empty());
            assertFalse(adminChangeQueueService.cancelByActionKey("key"));
            verify(repo, never()).delete(any());
        }

        @Test
        void cancelByActionKey_createCardSuccess_cleansUpAndDeletes() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder().actionType("create_card").payload("{\"tempImagePath\":\"path/to/image.jpg\"}").build();
            when(repo.findFirstByActionKey("key")).thenReturn(Optional.of(change));

            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class)))
                    .thenReturn(new PendingCardCreatePayload(null, "path/to/image.jpg"));

            // Act
            assertTrue(adminChangeQueueService.cancelByActionKey("key"));

            // Assert
            verify(imageService).deletePendingImage("path/to/image.jpg");
            verify(repo).delete(change);
        }

        @Test
        void cancelByActionKey_cleanupFails_stillDeletes() throws Exception {
            // Arrange
            AdminPendingChange change = AdminPendingChange.builder().actionType("update_card").payload("...").build();
            when(repo.findFirstByActionKey("key")).thenReturn(Optional.of(change));
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenThrow(new JsonProcessingException("Error") {});

            // Act
            assertTrue(adminChangeQueueService.cancelByActionKey("key"));

            // Assert
            verify(repo).delete(change);
        }
    }

    @Nested
    class ListPendingChangesTests {

        @Test
        void listPendingCardChanges_parsesAllTypesCorrectly() throws Exception {
            // Arrange
            AdminPendingChange create = AdminPendingChange.builder().actionType("create_card").payload("{\"card\":{\"name\":\"New\"}}").createdAt(Instant.EPOCH).build();
            AdminPendingChange update = AdminPendingChange.builder().actionType("update_card").payload("{\"id\":1, \"card\":{\"name\":\"Update\"}}").createdAt(Instant.EPOCH).build();
            AdminPendingChange delete = AdminPendingChange.builder().actionType("delete_card").payload("{\"id\":2}").createdAt(Instant.EPOCH).build();
            AdminPendingChange flush = AdminPendingChange.builder().actionType("flush_cache").payload("{}").createdAt(Instant.EPOCH).build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(create, update, delete, flush));

            AdminCharacterCardRequest mockNewCard = mock(AdminCharacterCardRequest.class);
            when(mockNewCard.name()).thenReturn("New");

            AdminCharacterCardRequest mockUpdateCard = mock(AdminCharacterCardRequest.class);
            when(mockUpdateCard.name()).thenReturn("Update");

            when(objectMapper.readValue(create.getPayload(), PendingCardCreatePayload.class))
                    .thenReturn(new PendingCardCreatePayload(mockNewCard, null));
            when(objectMapper.readValue(update.getPayload(), PendingCardUpdatePayload.class))
                    .thenReturn(new PendingCardUpdatePayload(1L, mockUpdateCard, null, null));
            when(objectMapper.readValue(delete.getPayload(), PendingCardDeletePayload.class))
                    .thenReturn(new PendingCardDeletePayload(2L));

            // Act
            List<AdminPendingCardChangeDto> result = adminChangeQueueService.listPendingCardChanges();

            // Assert
            assertEquals(3, result.size());
            assertEquals("create", result.get(0).changeType());
            assertEquals("New", result.get(0).cardName());
            assertEquals("update", result.get(1).changeType());
            assertEquals(1L, result.get(1).cardId());
            assertEquals("delete", result.get(2).changeType());
            assertEquals(2L, result.get(2).cardId());
        }

        @Test
        void listPendingCardChanges_parsingError_skipsBadEntry() throws Exception {
            // Arrange
            AdminPendingChange good = AdminPendingChange.builder().actionType("create_card").payload("{\"card\":{\"name\":\"Good\"}}").createdAt(Instant.EPOCH).build();
            AdminPendingChange bad = AdminPendingChange.builder().actionType("delete_card").payload("bad_json").createdAt(Instant.EPOCH).build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(good, bad));

            AdminCharacterCardRequest mockGoodCard = mock(AdminCharacterCardRequest.class);
            when(mockGoodCard.name()).thenReturn("Good");

            when(objectMapper.readValue(good.getPayload(), PendingCardCreatePayload.class))
                    .thenReturn(new PendingCardCreatePayload(mockGoodCard, null));
            when(objectMapper.readValue("bad_json", PendingCardDeletePayload.class))
                    .thenThrow(new JsonProcessingException("Error") {});

            // Act
            List<AdminPendingCardChangeDto> result = adminChangeQueueService.listPendingCardChanges();

            // Assert
            assertEquals(1, result.size());
            assertEquals("Good", result.getFirst().cardName());
        }
    }
}