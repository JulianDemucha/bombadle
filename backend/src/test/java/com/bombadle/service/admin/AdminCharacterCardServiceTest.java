package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.CharacterCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCharacterCardServiceTest {

    @InjectMocks
    private AdminCharacterCardService adminCharacterCardService;

    @Mock
    private CharacterCardRepository characterCardRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private AdminChangeQueueService changeQueueService;
    @Mock
    private CharacterCardImageService imageService;
    @Mock
    private MultipartFile mockImage;

    @Test
    void enqueueCreate_validRequest_enqueuesTaskAndLogsAction() throws IOException {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("sigma_name", "MALE", null, null, null, null, null, null);
        when(characterCardRepository.existsByName("sigma_name")).thenReturn(false);
        when(changeQueueService.hasPendingCardName("sigma_name", null)).thenReturn(false);
        when(imageService.buildSlug("sigma_name")).thenReturn("sigma-name");
        when(changeQueueService.hasPendingActionKey("card:create:sigma-name")).thenReturn(false);
        when(imageService.storePendingImage(mockImage)).thenReturn("sigma/path.jpg");

        adminCharacterCardService.enqueueCreate(1L, request, mockImage);

        verify(changeQueueService).enqueue(eq("create_card"), eq("card:create:sigma-name"), any());
        verify(adminAuditService).logAction(1L, "create_card_pending", null);
    }

    @Test
    void enqueueCreate_nullRequest_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueCreate(1L, null, mockImage)
        );
    }

    @Test
    void enqueueCreate_missingName_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest(null, "MALE", null, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueCreate(1L, request, mockImage)
        );
    }

    @Test
    void enqueueCreate_missingGender_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("sigma_name", null, null, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueCreate(1L, request, mockImage)
        );
    }

    @Test
    void enqueueCreate_nameAlreadyExistsInRepo_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("beta_name", "MALE", null, null, null, null, null, null);
        when(characterCardRepository.existsByName("beta_name")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueCreate(1L, request, mockImage)
        );
    }

    @Test
    void enqueueCreate_nameAlreadyPending_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("beta_name", "MALE", null, null, null, null, null, null);
        when(changeQueueService.hasPendingCardName("beta_name", null)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueCreate(1L, request, mockImage)
        );
    }

    @Test
    void enqueueCreate_actionKeyAlreadyPending_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("sigma_name", "MALE", null, null, null, null, null, null);
        when(imageService.buildSlug("sigma_name")).thenReturn("sigma-name");
        when(changeQueueService.hasPendingActionKey("card:create:sigma-name")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueCreate(1L, request, mockImage)
        );
    }

    @Test
    void enqueueUpdate_validRequestWithImage_enqueuesTaskAndLogsAction() throws IOException {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("sigma_updated", null, null, null, null, null, null, null);
        when(mockImage.isEmpty()).thenReturn(false);
        when(imageService.storePendingImage(mockImage)).thenReturn("sigma/path.jpg");

        adminCharacterCardService.enqueueUpdate(1L, 10L, request, mockImage, "beta_old");

        verify(changeQueueService).enqueue(eq("update_card_10"), eq("card:10"), any());
        verify(adminAuditService).logAction(1L, "update_card_10", null);
    }

    @Test
    void enqueueUpdate_validRequestWithoutImage_enqueuesTaskWithoutImage() throws IOException {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("sigma_updated", null, null, null, null, null, null, null);

        adminCharacterCardService.enqueueUpdate(1L, 10L, request, null, "beta_old");

        verify(imageService, never()).storePendingImage(any());
        verify(changeQueueService).enqueue(eq("update_card_10"), eq("card:10"), any());
        verify(adminAuditService).logAction(1L, "update_card_10", null);
    }

    @Test
    void enqueueUpdate_nameNotChanged_skipsDbExistenceCheck() throws IOException {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("sigma_same", null, null, null, null, null, null, null);

        adminCharacterCardService.enqueueUpdate(1L, 10L, request, null, "sigma_same");

        verify(characterCardRepository, never()).existsByName(anyString());
        verify(changeQueueService).enqueue(eq("update_card_10"), eq("card:10"), any());
    }

    @Test
    void enqueueUpdate_nullRequest_throwsException() {
        assertThrows(AdminOperationNotAllowedException.class, () ->
                adminCharacterCardService.enqueueUpdate(1L, 10L, null, mockImage, "beta_old")
        );
    }

    @Test
    void enqueueUpdate_newNameExistsInRepo_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("beta_taken", null, null, null, null, null, null, null);
        when(characterCardRepository.existsByName("beta_taken")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueUpdate(1L, 10L, request, mockImage, "beta_old")
        );
    }

    @Test
    void enqueueUpdate_newNameIsPending_throwsIllegalArgumentException() {
        AdminCharacterCardRequest request = new AdminCharacterCardRequest("beta_pending", null, null, null, null, null, null, null);
        when(changeQueueService.hasPendingCardName("beta_pending", 10L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                adminCharacterCardService.enqueueUpdate(1L, 10L, request, mockImage, "beta_old")
        );
    }

    @Test
    void enqueueDelete_validId_enqueuesTaskAndLogsAction() {
        adminCharacterCardService.enqueueDelete(1L, 10L);
        verify(changeQueueService).enqueue(eq("delete_card_10"), eq("card:10"), any());
        verify(adminAuditService).logAction(1L, "delete_card_10", null);
    }

    @Test
    void cancelCreate_validName_cancelsAndLogs() {
        when(imageService.buildSlug("sigma_card")).thenReturn("sigma-card");
        when(changeQueueService.cancelByActionKey("card:create:sigma-card")).thenReturn(true);

        adminCharacterCardService.cancelCreate(1L, "sigma_card");

        verify(changeQueueService).cancelByActionKey("card:create:sigma-card");
        verify(adminAuditService).logAction(1L, "cancel_create_card_sigma-card", null);
    }

    @Test
    void cancelCreate_notFound_throwsIllegalArgumentException() {
        when(imageService.buildSlug("beta_not_found")).thenReturn("beta-not-found");
        when(changeQueueService.cancelByActionKey("card:create:beta-not-found")).thenReturn(false);

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

    @Test
    void listPendingChanges_callsChangeQueueService() {
        adminCharacterCardService.listPendingChanges();
        verify(changeQueueService).listPendingCardChanges();
    }
}