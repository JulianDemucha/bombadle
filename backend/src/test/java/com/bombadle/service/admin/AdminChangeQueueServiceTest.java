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

    @Mock private AdminPendingChangeRepository repo;
    @Mock private AdminCharacterCardProcessor processor;
    @Mock private CharacterCardImageService imageService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private AdminChangeQueueService adminChangeQueueService;

    // ── applyAll ──────────────────────────────────────────────────────────────

    @Nested
    class ApplyAllTests {

        @Test
        void noChanges_doesNothing() {
            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of());

            adminChangeQueueService.applyAll();

            verifyNoInteractions(processor);
        }

        @Test
        void validChanges_processesAndDeletesAll() throws Exception {
            AdminPendingChange change1 = AdminPendingChange.builder().id(1L).actionType("create_card").payload("{}").build();
            AdminPendingChange change2 = AdminPendingChange.builder().id(2L).actionType("update_card").actionKey("card:12").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change1, change2));

            PendingCardCreatePayload createPayload = mock(PendingCardCreatePayload.class);
            PendingCardUpdatePayload updatePayload = mock(PendingCardUpdatePayload.class);

            when(objectMapper.readValue(change1.getPayload(), PendingCardCreatePayload.class)).thenReturn(createPayload);
            when(objectMapper.readValue(change2.getPayload(), PendingCardUpdatePayload.class)).thenReturn(updatePayload);

            adminChangeQueueService.applyAll();

            verify(processor).processCreate(createPayload);
            verify(processor).processUpdate(updatePayload);
            verify(repo).delete(change1);
            verify(repo).delete(change2);
        }

        @Test
        void changeThrowsException_catchesExceptionAndStillDeletesTask() throws Exception {
            AdminPendingChange change1 = AdminPendingChange.builder().id(1L).actionType("create_card").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change1));
            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class))).thenThrow(new JsonProcessingException("Test Exception") {});

            assertDoesNotThrow(() -> adminChangeQueueService.applyAll());

            verify(processor, never()).processCreate(any());
            verify(repo).delete(change1);
        }

        @Test
        void multipleChangesWithOneFailure_processesOthersAndDeletesAll() throws Exception {
            AdminPendingChange badChange = AdminPendingChange.builder().id(1L).actionType("create_card").payload("bad").build();
            AdminPendingChange goodChange = AdminPendingChange.builder().id(2L).actionType("delete_card").actionKey("card:5").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(badChange, goodChange));
            when(objectMapper.readValue("bad", PendingCardCreatePayload.class)).thenThrow(new JsonProcessingException("Test Exception") {});

            PendingCardDeletePayload deletePayload = mock(PendingCardDeletePayload.class);
            when(objectMapper.readValue(goodChange.getPayload(), PendingCardDeletePayload.class)).thenReturn(deletePayload);

            assertDoesNotThrow(() -> adminChangeQueueService.applyAll());

            verify(processor, never()).processCreate(any());
            verify(processor).processDelete(deletePayload);
            verify(repo).delete(badChange);
            verify(repo).delete(goodChange);
        }
    }

    // ── enqueue ───────────────────────────────────────────────────────────────

    @Nested
    class EnqueueTests {

        @Test
        void existingActionKey_cleansUpOldPayloadBeforeOverwriting() throws Exception {
            String oldJson = "{\"old\":true}";
            AdminPendingChange existing = AdminPendingChange.builder()
                    .id(1L).actionType("update_card").actionKey("card:5").payload(oldJson).build();

            when(repo.findFirstByActionKey("card:5")).thenReturn(Optional.of(existing));
            when(objectMapper.writeValueAsString(any())).thenReturn("new_json");

            PendingCardUpdatePayload oldPayload = mock(PendingCardUpdatePayload.class);
            when(oldPayload.tempImagePath()).thenReturn("old/img.jpg");
            when(oldPayload.tempGuessImagePath()).thenReturn("old/guess.jpg");
            when(objectMapper.readValue(oldJson, PendingCardUpdatePayload.class)).thenReturn(oldPayload);

            adminChangeQueueService.enqueue("delete_card_5", "card:5", new PendingCardDeletePayload(5L));

            verify(imageService).deletePendingImage("old/img.jpg");
            verify(imageService).deletePendingImage("old/guess.jpg");
            verify(repo).save(existing);
        }

        @Test
        void newActionKey_createsNewEntry() throws Exception {
            when(repo.findFirstByActionKey("card:create:sigma")).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            adminChangeQueueService.enqueue("create_card", "card:create:sigma", new PendingCardDeletePayload(1L));

            verify(repo).save(any(AdminPendingChange.class));
            verify(imageService, never()).deletePendingImage(anyString());
        }
    }

    // ── listPendingChanges ────────────────────────────────────────────────────

    @Nested
    class ListPendingChangesTests {

        @Test
        void returnsMappedList() throws Exception {
            AdminPendingChange change = AdminPendingChange.builder().id(1L).actionType("update_card").actionKey("card:12").payload("{}").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change));
            PendingCardUpdatePayload updatePayload = mock(PendingCardUpdatePayload.class);
            when(updatePayload.id()).thenReturn(12L);
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenReturn(updatePayload);

            List<AdminPendingCardChangeDto> result = adminChangeQueueService.listPendingCardChanges();

            assertEquals(1, result.size());
            assertEquals(12L, result.getFirst().cardId());
            assertEquals("update_card", result.getFirst().actionType());
        }

        @Test
        void parsingError_skipsBadEntry() throws Exception {
            AdminPendingChange change = AdminPendingChange.builder().id(1L).actionType("create_card").payload("invalid").build();

            when(repo.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(change));
            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class))).thenThrow(new JsonProcessingException("Parsing Error") {});

            List<AdminPendingCardChangeDto> result = adminChangeQueueService.listPendingCardChanges();

            assertTrue(result.isEmpty());
        }
    }

    // ── cancelByActionKey ─────────────────────────────────────────────────────

    @Nested
    class CancelTests {

        @Test
        void updateWithBothImages_deletesBothStagedFiles() throws Exception {
            AdminPendingChange updateChange = AdminPendingChange.builder()
                    .id(2L).actionType("update_card").actionKey("card:10").payload("{}").build();

            when(repo.findFirstByActionKey("card:10")).thenReturn(Optional.of(updateChange));

            PendingCardUpdatePayload updatePayload = mock(PendingCardUpdatePayload.class);
            when(updatePayload.tempImagePath()).thenReturn("temp/display.jpg");
            when(updatePayload.tempGuessImagePath()).thenReturn("temp/guess.jpg");
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenReturn(updatePayload);

            boolean result = adminChangeQueueService.cancelByActionKey("card:10");

            assertTrue(result);
            verify(imageService).deletePendingImage("temp/display.jpg");
            verify(imageService).deletePendingImage("temp/guess.jpg");
            verify(repo).delete(updateChange);
        }

        @Test
        void createWithBothImages_deletesBothStagedFiles() throws Exception {
            AdminPendingChange createChange = AdminPendingChange.builder()
                    .id(1L).actionType("create_card").actionKey("card:create:sigma").payload("{}").build();

            when(repo.findFirstByActionKey("card:create:sigma")).thenReturn(Optional.of(createChange));

            PendingCardCreatePayload createPayload = mock(PendingCardCreatePayload.class);
            when(createPayload.tempImagePath()).thenReturn("temp/display.jpg");
            when(createPayload.tempGuessImagePath()).thenReturn("temp/guess.jpg");
            when(objectMapper.readValue(anyString(), eq(PendingCardCreatePayload.class))).thenReturn(createPayload);

            boolean result = adminChangeQueueService.cancelByActionKey("card:create:sigma");

            assertTrue(result);
            verify(imageService).deletePendingImage("temp/display.jpg");
            verify(imageService).deletePendingImage("temp/guess.jpg");
            verify(repo).delete(createChange);
        }

        @Test
        void cleanupFails_stillDeletes() throws Exception {
            AdminPendingChange updateChange = AdminPendingChange.builder()
                    .id(2L).actionType("update_card").actionKey("card:10").payload("{}").build();

            when(repo.findFirstByActionKey("card:10")).thenReturn(Optional.of(updateChange));
            when(objectMapper.readValue(anyString(), eq(PendingCardUpdatePayload.class))).thenThrow(new JsonProcessingException("Test Exception") {});

            boolean result = adminChangeQueueService.cancelByActionKey("card:10");

            assertTrue(result);
            verify(imageService, never()).deletePendingImage(anyString());
            verify(repo).delete(updateChange);
        }

        @Test
        void notFound_returnsFalse() {
            when(repo.findFirstByActionKey("card:10")).thenReturn(Optional.empty());

            boolean result = adminChangeQueueService.cancelByActionKey("card:10");

            assertFalse(result);
            verify(repo, never()).delete(any());
        }
    }
}
