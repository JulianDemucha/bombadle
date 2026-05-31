package com.bombadle.service.admin;

import com.bombadle.dto.queue.PendingCacheFlushPayload;
import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Color;
import com.bombadle.enums.Gender;
import com.bombadle.enums.Race;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCharacterCardProcessorTest {

    @InjectMocks
    private AdminCharacterCardProcessor processor;

    @Mock
    private CharacterCardRepository repository;

    @Mock
    private CharacterCardImageService imageService;

    @Mock
    private CacheService cacheService;

    @Test
    void processCreate_nameAlreadyExists_throwsIllegalArgumentException() {
        AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                "sigma_name",
                "MALE",
                "Czlowiek",
                true, Set.of("ZIELONY"),
                Set.of("Gwiezdna_Flota"),
                1, Set.of()
        );
        PendingCardCreatePayload payload = new PendingCardCreatePayload(req, "sigma/path.jpg");
        when(repository.existsByName("sigma_name")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                processor.processCreate(payload)
        );

        assertEquals("Character card name already exists: sigma_name", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void processCreate_validPayloadWithTempImage_savesCardAndAppliesImage() throws IOException {
        AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                "sigma_card",
                "MALE",
                "Czlowiek",
                true, Set.of("ZIELONY"),
                Set.of("Gwiezdna_Flota"),
                1, Set.of("sigma_alias")
        );
        PendingCardCreatePayload payload = new PendingCardCreatePayload(req, "sigma/path.jpg");
        when(repository.existsByName("sigma_card")).thenReturn(false);
        when(imageService.buildImageSrc("sigma_card")).thenReturn("/images/sigma-card.png");

        processor.processCreate(payload);

        ArgumentCaptor<CharacterCard> captor = ArgumentCaptor.forClass(CharacterCard.class);
        verify(repository).save(captor.capture());
        CharacterCard savedCard = captor.getValue();

        assertEquals("sigma_card", savedCard.getName());
        assertEquals("/images/sigma-card.png", savedCard.getImageSrc());
        assertEquals(Gender.MALE, savedCard.getGender());
        assertEquals(Race.Czlowiek, savedCard.getRace());
        assertTrue(savedCard.getAlive());
        assertTrue(savedCard.getColors().contains(Color.ZIELONY));
        assertTrue(savedCard.getAffiliations().contains(Affiliation.Gwiezdna_Flota));
        assertEquals(1, savedCard.getFirstAppearanceEpisode());
        assertTrue(savedCard.getAliases().contains("sigma_alias"));

        verify(imageService).applyPendingImage("sigma/path.jpg", "sigma_card");
    }

    @Test
    void processCreate_validPayloadWithoutTempImage_savesCardWithoutApplyingImage() throws IOException {
        AdminCharacterCardRequest req = new AdminCharacterCardRequest(
                "beta_card", null, null, null, null, null, null, null
        );
        PendingCardCreatePayload payload = new PendingCardCreatePayload(req, null);
        when(repository.existsByName("beta_card")).thenReturn(false);
        when(imageService.buildImageSrc("beta_card")).thenReturn("/images/beta-card.png");

        processor.processCreate(payload);

        verify(repository).save(any(CharacterCard.class));
        verify(imageService, never()).applyPendingImage(anyString(), anyString());
        verify(imageService, never()).renameImage(anyString(), anyString());
    }

    @Test
    void processUpdate_cardNotFound_throwsIllegalArgumentException() {
        AdminCharacterCardRequest req = new AdminCharacterCardRequest("sigma_name", null, null,
                null, null, null, null, null);
        PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, req, "sigma_temp.jpg", "beta_name");
        when(repository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                processor.processUpdate(payload)
        );

        assertEquals("Character card not found: 1", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void processUpdate_newNameAlreadyExists_throwsIllegalArgumentException() {
        CharacterCard existingCard = CharacterCard.create();
        existingCard.setName("beta_name");
        AdminCharacterCardRequest req = new AdminCharacterCardRequest("sigma_name", null, null,
                null, null, null, null, null);
        PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, req, null, "beta_name");

        when(repository.findById(1L)).thenReturn(Optional.of(existingCard));
        when(repository.existsByName("sigma_name")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                processor.processUpdate(payload)
        );

        assertEquals("Character card name already exists: sigma_name", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void processUpdate_validPayloadWithTempImage_updatesCardAndAppliesImage() throws IOException {
        CharacterCard existingCard = CharacterCard.create();
        existingCard.setName("beta_name");
        AdminCharacterCardRequest req = new AdminCharacterCardRequest("sigma_name", "FEMALE", null,
                null, null, null, null, null);
        PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, req, "sigma_temp.jpg", "beta_name");

        when(repository.findById(1L)).thenReturn(Optional.of(existingCard));
        when(repository.existsByName("sigma_name")).thenReturn(false);
        when(imageService.buildImageSrc("sigma_name")).thenReturn("/images/sigma-name.png");

        processor.processUpdate(payload);

        verify(repository).save(existingCard);
        assertEquals("sigma_name", existingCard.getName());
        assertEquals(Gender.FEMALE, existingCard.getGender());
        assertEquals("/images/sigma-name.png", existingCard.getImageSrc());

        verify(imageService).applyPendingImage("sigma_temp.jpg", "sigma_name");
        verify(imageService, never()).renameImage(anyString(), anyString());
    }

    @Test
    void processUpdate_nameChangedWithoutTempImage_updatesCardAndRenamesImage() throws IOException {
        CharacterCard existingCard = CharacterCard.create();
        existingCard.setName("beta_name");
        AdminCharacterCardRequest req = new AdminCharacterCardRequest("sigma_name", null, null,
                null, null, null, null, null);
        PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, req, null, "beta_name");

        when(repository.findById(1L)).thenReturn(Optional.of(existingCard));
        when(repository.existsByName("sigma_name")).thenReturn(false);
        when(imageService.buildImageSrc("sigma_name")).thenReturn("/images/sigma-name.png");

        processor.processUpdate(payload);

        verify(repository).save(existingCard);
        verify(imageService, never()).applyPendingImage(anyString(), anyString());
        verify(imageService).renameImage("beta_name", "sigma_name");
    }

    @Test
    void processUpdate_nameNotProvided_keepsOldNameAndUpdatesOtherFields() throws IOException {
        CharacterCard existingCard = CharacterCard.create();
        existingCard.setName("sigma_name");
        AdminCharacterCardRequest req = new AdminCharacterCardRequest(" ", "MALE", null,
                null, null, null, null, null);
        PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, req, null, "sigma_name");

        when(repository.findById(1L)).thenReturn(Optional.of(existingCard));
        when(imageService.buildImageSrc("sigma_name")).thenReturn("/images/sigma-name.png");

        processor.processUpdate(payload);

        verify(repository, never()).existsByName(anyString());
        verify(repository).save(existingCard);
        assertEquals("sigma_name", existingCard.getName());
        assertEquals(Gender.MALE, existingCard.getGender());

        verify(imageService, never()).applyPendingImage(anyString(), anyString());
        verify(imageService, never()).renameImage(anyString(), anyString());
    }

    @Test
    void processDelete_cardExists_deletesCard() {
        PendingCardDeletePayload payload = new PendingCardDeletePayload(1L);
        when(repository.existsById(1L)).thenReturn(true);

        processor.processDelete(payload);

        verify(repository).deleteById(1L);
    }

    @Test
    void processDelete_cardDoesNotExist_doesNothing() {
        PendingCardDeletePayload payload = new PendingCardDeletePayload(1L);
        when(repository.existsById(1L)).thenReturn(false);

        processor.processDelete(payload);

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void processCacheFlush_flushAllIsTrue_evictsAllCaches() {
        PendingCacheFlushPayload payload = new PendingCacheFlushPayload(null, true);

        processor.processCacheFlush(payload);

        verify(cacheService).evictAllCaches();
        verify(cacheService, never()).evictCache(anyString());
    }

    @Test
    void processCacheFlush_flushAllIsFalseAndCacheNameProvided_evictsSpecificCache() {
        PendingCacheFlushPayload payload = new PendingCacheFlushPayload("sigma_cache", false);

        processor.processCacheFlush(payload);

        verify(cacheService, never()).evictAllCaches();
        verify(cacheService).evictCache("sigma_cache");
    }

    @Test
    void processCacheFlush_flushAllIsFalseAndCacheNameIsBlank_doesNothing() {
        PendingCacheFlushPayload payload = new PendingCacheFlushPayload(" ", false);

        processor.processCacheFlush(payload);

        verify(cacheService, never()).evictAllCaches();
        verify(cacheService, never()).evictCache(anyString());
    }
}