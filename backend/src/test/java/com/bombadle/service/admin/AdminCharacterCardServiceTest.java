package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.dto.request.AdminCharacterCardUpdateRequest;
import com.bombadle.dto.request.AdminQuoteRequest;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.CharacterCardRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCharacterCardServiceTest {

    @InjectMocks
    private AdminCharacterCardService adminCharacterCardService;

    @Mock private CharacterCardRepository characterCardRepository;
    @Mock private AdminAuditService adminAuditService;
    @Mock private AdminChangeQueueService changeQueueService;
    @Mock private CharacterCardImageService imageService;
    @Mock private MultipartFile mockImage;
    @Mock private MultipartFile mockGuessImage;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AdminQuoteRequest validQuote() {
        return new AdminQuoteRequest("Who said it?", List.of("A", "B"), "A", "SPEAKER", 1);
    }

    private AdminCharacterCardRequest createRequest(String name) {
        return new AdminCharacterCardRequest(name, "MALE", null, null, null, null, null, null,
                List.of(validQuote()));
    }

    private AdminCharacterCardUpdateRequest updateRequest(String name) {
        return new AdminCharacterCardUpdateRequest(name, null, null, null, null, null, null, null,
                null, null);
    }

    // ── enqueueCreate ─────────────────────────────────────────────────────────

    @Nested
    class EnqueueCreateTests {

        @Test
        void validRequest_enqueuesTaskAndLogsAction() throws IOException {
            when(characterCardRepository.existsByName("sigma_name")).thenReturn(false);
            when(changeQueueService.hasPendingCardName("sigma_name", null)).thenReturn(false);
            when(imageService.buildSlug("sigma_name")).thenReturn("sigma_name");
            when(changeQueueService.hasPendingActionKey("card:create:sigma_name")).thenReturn(false);
            when(imageService.storePendingImage(mockImage)).thenReturn("sigma/path.jpg");
            when(imageService.storePendingGuessImage(mockGuessImage)).thenReturn("sigma/guess.jpg");

            adminCharacterCardService.enqueueCreate(1L, createRequest("sigma_name"), mockImage, mockGuessImage);

            verify(changeQueueService).enqueue(eq("create_card"), eq("card:create:sigma_name"), any());
            verify(adminAuditService).logAction(1L, "create_card_pending", "sigma_name");
        }

        @Test
        void nullRequest_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, null, mockImage, mockGuessImage)
            );
        }

        @Test
        void missingName_throwsIllegalArgumentException() {
            AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                    null, "MALE", null, null, null, null, null, null, List.of(validQuote()));
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, req, mockImage, mockGuessImage)
            );
        }

        @Test
        void missingGender_throwsIllegalArgumentException() {
            AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                    "sigma_name", null, null, null, null, null, null, null, List.of(validQuote()));
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, req, mockImage, mockGuessImage)
            );
        }

        @Test
        void noQuotes_throwsIllegalArgumentException() {
            AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                    "sigma_name", "MALE", null, null, null, null, null, null, null);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, req, mockImage, mockGuessImage)
            );
        }

        @Test
        void quoteWithWrongAnswer_throwsIllegalArgumentException() {
            AdminQuoteRequest badQuote = new AdminQuoteRequest("Q?", List.of("A", "B"), "C", "SPEAKER", 1);
            AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                    "sigma_name", "MALE", null, null, null, null, null, null, List.of(badQuote));
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, req, mockImage, mockGuessImage)
            );
        }

        @Test
        void nameAlreadyExistsInRepo_throwsIllegalArgumentException() {
            when(characterCardRepository.existsByName("beta_name")).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, createRequest("beta_name"), mockImage, mockGuessImage)
            );
        }

        @Test
        void nameAlreadyPending_throwsIllegalArgumentException() {
            when(changeQueueService.hasPendingCardName("beta_name", null)).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, createRequest("beta_name"), mockImage, mockGuessImage)
            );
        }

        @Test
        void actionKeyAlreadyPending_throwsIllegalArgumentException() {
            when(imageService.buildSlug("sigma_name")).thenReturn("sigma_name");
            when(changeQueueService.hasPendingActionKey("card:create:sigma_name")).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueCreate(1L, createRequest("sigma_name"), mockImage, mockGuessImage)
            );
        }
    }

    // ── enqueueUpdate ─────────────────────────────────────────────────────────

    @Nested
    class EnqueueUpdateTests {

        @Test
        void validRequestWithBothImages_enqueuesTaskAndLogsAction() throws IOException {
            when(mockImage.isEmpty()).thenReturn(false);
            when(mockGuessImage.isEmpty()).thenReturn(false);
            when(imageService.storePendingImage(mockImage)).thenReturn("sigma/path.jpg");
            when(imageService.storePendingGuessImage(mockGuessImage)).thenReturn("sigma/guess.jpg");

            adminCharacterCardService.enqueueUpdate(1L, 10L, updateRequest("sigma_updated"), mockImage, mockGuessImage, "beta_old");

            verify(changeQueueService).enqueue(eq("update_card_10"), eq("card:10"), any());
            verify(adminAuditService).logAction(1L, "update_card_10", null);
        }

        @Test
        void noImages_enqueuesWithoutStagingFiles() throws IOException {
            adminCharacterCardService.enqueueUpdate(1L, 10L, updateRequest("sigma_updated"), null, null, "beta_old");

            verify(imageService, never()).storePendingImage(any());
            verify(imageService, never()).storePendingGuessImage(any());
            verify(changeQueueService).enqueue(eq("update_card_10"), eq("card:10"), any());
        }

        @Test
        void nameNotChanged_skipsDbExistenceCheck() throws IOException {
            adminCharacterCardService.enqueueUpdate(1L, 10L, updateRequest("same_name"), null, null, "same_name");

            verify(characterCardRepository, never()).existsByName(anyString());
            verify(changeQueueService).enqueue(eq("update_card_10"), eq("card:10"), any());
        }

        @Test
        void nullRequest_throwsException() {
            assertThrows(AdminOperationNotAllowedException.class, () ->
                    adminCharacterCardService.enqueueUpdate(1L, 10L, null, mockImage, mockGuessImage, "beta_old")
            );
        }

        @Test
        void newNameExistsInRepo_throwsIllegalArgumentException() {
            when(characterCardRepository.existsByName("beta_taken")).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueUpdate(1L, 10L, updateRequest("beta_taken"), mockImage, mockGuessImage, "beta_old")
            );
        }

        @Test
        void newNameIsPending_throwsIllegalArgumentException() {
            when(changeQueueService.hasPendingCardName("beta_pending", 10L)).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueUpdate(1L, 10L, updateRequest("beta_pending"), mockImage, mockGuessImage, "beta_old")
            );
        }

        @Test
        void quotesToAddWithInvalidAnswer_throwsIllegalArgumentException() {
            AdminQuoteRequest badQuote = new AdminQuoteRequest("Q?", List.of("A", "B"), "C", "SPEAKER", 1);
            AdminCharacterCardUpdateRequest req = new AdminCharacterCardUpdateRequest(
                    null, null, null, null, null, null, null, null, List.of(badQuote), null);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.enqueueUpdate(1L, 10L, req, null, null, "old_name")
            );
        }
    }

    // ── enqueueDelete ─────────────────────────────────────────────────────────

    @Nested
    class EnqueueDeleteTests {

        @Test
        void validId_enqueuesTaskAndLogsAction() {
            adminCharacterCardService.enqueueDelete(1L, 10L);
            verify(changeQueueService).enqueue(eq("delete_card_10"), eq("card:10"), any());
            verify(adminAuditService).logAction(1L, "delete_card_10", null);
        }
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Nested
    class CancelTests {

        @Test
        void cancelCreate_validName_cancelsAndLogs() {
            when(imageService.buildSlug("sigma_card")).thenReturn("sigma_card");
            when(changeQueueService.cancelByActionKey("card:create:sigma_card")).thenReturn(true);

            adminCharacterCardService.cancelCreate(1L, "sigma_card");

            verify(changeQueueService).cancelByActionKey("card:create:sigma_card");
            verify(adminAuditService).logAction(eq(1L), anyString(), isNull());
        }

        @Test
        void cancelCreate_notFound_throwsIllegalArgumentException() {
            when(imageService.buildSlug("beta_not_found")).thenReturn("beta_not_found");
            when(changeQueueService.cancelByActionKey("card:create:beta_not_found")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.cancelCreate(1L, "beta_not_found")
            );
        }

        @Test
        void cancelUpdate_validId_cancelsAndLogs() {
            when(changeQueueService.cancelByActionKey("card:10")).thenReturn(true);

            adminCharacterCardService.cancelUpdate(1L, 10L);

            verify(changeQueueService).cancelByActionKey("card:10");
            verify(adminAuditService).logAction(1L, "cancel_update_card_10", null);
        }

        @Test
        void cancelUpdate_notFound_throwsIllegalArgumentException() {
            when(changeQueueService.cancelByActionKey("card:10")).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.cancelUpdate(1L, 10L)
            );
        }

        @Test
        void cancelDelete_validId_cancelsAndLogs() {
            when(changeQueueService.cancelByActionKey("card:10")).thenReturn(true);

            adminCharacterCardService.cancelDelete(1L, 10L);

            verify(changeQueueService).cancelByActionKey("card:10");
            verify(adminAuditService).logAction(1L, "cancel_delete_card_10", null);
        }

        @Test
        void cancelDelete_notFound_throwsIllegalArgumentException() {
            when(changeQueueService.cancelByActionKey("card:10")).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () ->
                    adminCharacterCardService.cancelDelete(1L, 10L)
            );
        }
    }

    @Nested
    class ListPendingChangesTests {

        @Test
        void listPendingChanges_delegatesToQueueService() {
            adminCharacterCardService.listPendingChanges();
            verify(changeQueueService).listPendingCardChanges();
        }
    }
}
