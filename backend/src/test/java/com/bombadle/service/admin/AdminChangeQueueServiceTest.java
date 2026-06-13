package com.bombadle.service.admin;

import com.bombadle.dto.AdminPendingCardChangeDto;
import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.bombadle.entity.AdminPendingChange;
import com.bombadle.repository.AdminPendingChangeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminChangeQueueServiceTest {

    @Mock
    private AdminPendingChangeRepository repo;

    @Mock
    private AdminCharacterCardProcessor processor;

    @Mock
    private CharacterCardImageService imageService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminChangeQueueService adminChangeQueueService;

    @Nested
    class ApplyAllTests {

        @Test
        void applyAll_noChanges_doesNothing() {
            // ARRANGE
            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of());

            // ACT
            adminChangeQueueService.applyAll();

            // ASSERT
            verifyNoInteractions(processor);
        }

        @Test
        void applyAll_validChanges_processesAndDeletesAll() throws Exception {
            // ARRANGE
            AdminPendingChange change1 = AdminPendingChange.builder().id(1L).actionType("create_card").payload("{}").build();
            AdminPendingChange change2 = AdminPendingChange.builder().id(2L).actionType("update_card").actionKey("12").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change1, change2));

            PendingCardCreatePayload createPayload = mock(PendingCardCreatePayload.class);
            PendingCardUpdatePayload updatePayload = mock(PendingCardUpdatePayload.class);

            when(objectMapper.readValue(change1.getPayload(), PendingCardCreatePayload.class)).thenReturn(createPayload);
            when(objectMapper.readValue(change2.getPayload(), PendingCardUpdatePayload.class)).thenReturn(updatePayload);

            // ACT
            adminChangeQueueService.applyAll();

            // ASSERT
            verify(processor).processCreate(createPayload);
            verify(processor).processUpdate(updatePayload);
            verify(repo).delete(change1);
            verify(repo).delete(change2);
        }

        @Test
        void applyAll_changeThrowsException_catchesExceptionAndStillDeletesTask() throws Exception {
            // ARRANGE
            AdminPendingChange change1 = AdminPendingChange.builder().id(1L).actionType("create_card").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change1));
            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class))).thenThrow(new JsonProcessingException("Test Exception") {});

            // ACT
            assertDoesNotThrow(() -> adminChangeQueueService.applyAll());

            // ASSERT
            verify(processor, never()).processCreate(any());
            verify(repo).delete(change1);
        }

        @Test
        void applyAll_multipleChangesWithOneFailure_processesOthersAndDeletesAll() throws Exception {
            // ARRANGE
            AdminPendingChange badChange = AdminPendingChange.builder().id(1L).actionType("create_card").payload("bad").build();
            AdminPendingChange goodChange = AdminPendingChange.builder().id(2L).actionType("delete_card").actionKey("5").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(badChange, goodChange));
            when(objectMapper.readValue("bad", PendingCardCreatePayload.class)).thenThrow(new JsonProcessingException("Test Exception") {});

            PendingCardDeletePayload deletePayload = mock(PendingCardDeletePayload.class);
            when(objectMapper.readValue(goodChange.getPayload(), PendingCardDeletePayload.class)).thenReturn(deletePayload);

            // ACT
            assertDoesNotThrow(() -> adminChangeQueueService.applyAll());

            // ASSERT
            verify(processor, never()).processCreate(any());
            verify(processor).processDelete(deletePayload);
            verify(repo).delete(badChange);
            verify(repo).delete(goodChange);
        }
    }

    @Nested
    class ListPendingChangesTests {

        @Test
        void listPendingCardChanges_returnsMappedList() throws Exception {
            // ARRANGE
            AdminPendingChange change = AdminPendingChange.builder().id(1L).actionType("update_card").actionKey("12").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change));
            PendingCardUpdatePayload updatePayload = mock(PendingCardUpdatePayload.class);
            when(updatePayload.id()).thenReturn(12L);
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenReturn(updatePayload);

            // ACT
            List<AdminPendingCardChangeDto> result = adminChangeQueueService.listPendingCardChanges();

            // ASSERT
            assertEquals(1, result.size());
            assertNotNull(result.getFirst());
            assertEquals(12L, result.getFirst().cardId());
            assertEquals("update_card", result.getFirst().actionType());
        }

        @Test
        void listPendingCardChanges_parsingError_skipsBadEntry() throws Exception {
            // ARRANGE
            AdminPendingChange change = AdminPendingChange.builder().id(1L).actionType("create_card").payload("invalid").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change));
            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class))).thenThrow(new JsonProcessingException("Parsing Error") {});

            // ACT
            List<AdminPendingCardChangeDto> result = adminChangeQueueService.listPendingCardChanges();

            // ASSERT
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class CancelTests {

        @Test
        void cancelByActionKey_findsAndDeletesChangeAndCleansPayload() throws Exception {
            // ARRANGE
            AdminPendingChange updateChange = AdminPendingChange.builder().id(2L).actionType("update_card").actionKey("10").payload("{}").build();

            when(repo.findFirstByActionKey("10")).thenReturn(Optional.of(updateChange));

            PendingCardUpdatePayload updatePayload = mock(PendingCardUpdatePayload.class);
            when(updatePayload.tempImagePath()).thenReturn("temp/path.png");
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenReturn(updatePayload);

            // ACT
            boolean result = adminChangeQueueService.cancelByActionKey("10");

            // ASSERT
            assertTrue(result);
            verify(imageService).deletePendingImage("temp/path.png");
            verify(repo).delete(updateChange);
        }

        @Test
        void cancelByActionKey_cleanupFails_stillDeletes() throws Exception {
            // ARRANGE
            AdminPendingChange updateChange = AdminPendingChange.builder().id(2L).actionType("update_card").actionKey("10").payload("{}").build();

            when(repo.findFirstByActionKey("10")).thenReturn(Optional.of(updateChange));
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenThrow(new JsonProcessingException("Test Exception") {});

            // ACT
            boolean result = adminChangeQueueService.cancelByActionKey("10");

            // ASSERT
            assertTrue(result);
            verify(imageService, never()).deletePendingImage(anyString());
            verify(repo).delete(updateChange);
        }

        @Test
        void cancelByActionKey_notFound_returnsFalse() {
            // ARRANGE
            when(repo.findFirstByActionKey("10")).thenReturn(Optional.empty());

            // ACT
            boolean result = adminChangeQueueService.cancelByActionKey("10");

            // ASSERT
            assertFalse(result);
            verify(repo, never()).delete(any());
        }
    }
}
