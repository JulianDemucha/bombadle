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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCharacterCardProcessor {

    private final CharacterCardRepository characterCardRepository;
    private final CharacterCardImageService imageService;
    private final CacheService cacheService;

    @Transactional
    public void processCreate(PendingCardCreatePayload payload) throws IOException {
        AdminCharacterCardRequest req = payload.card();

        if (characterCardRepository.existsByName(req.name())) {
            throw new IllegalArgumentException("Character card name already exists: " + req.name());
        }

        CharacterCard card = CharacterCard.create();
        finalizeCardProcessing(card, req, req.name(), payload.tempImagePath(), null);

        log.info("Successfully processed pending creation for card: {}", req.name());
    }

    @Transactional
    public void processUpdate(PendingCardUpdatePayload payload) throws IOException {
        CharacterCard card = characterCardRepository.findById(payload.id())
                .orElseThrow(() -> new IllegalArgumentException("Character card not found: " + payload.id()));

        AdminCharacterCardRequest req = payload.card();
        String nextName = req.name() != null && !req.name().isBlank() ? req.name() : card.getName();

        if (!nextName.equals(card.getName()) && characterCardRepository.existsByName(nextName)) {
            throw new IllegalArgumentException("Character card name already exists: " + nextName);
        }

        finalizeCardProcessing(card, req, nextName, payload.tempImagePath(), payload.previousName());

        log.info("Successfully processed pending update for card ID: {}", payload.id());
    }

    @Transactional
    public void processDelete(PendingCardDeletePayload payload) {
        if (!characterCardRepository.existsById(payload.id())) {
            log.warn("Attempted to delete card ID {}, but it does not exist", payload.id());
            return;
        }
        characterCardRepository.deleteById(payload.id());
        log.info("Successfully processed pending deletion for card ID: {}", payload.id());
    }

    public void processCacheFlush(PendingCacheFlushPayload payload) {
        if (Boolean.TRUE.equals(payload.flushAll())) {
            cacheService.evictAllCaches();
            log.info("Successfully processed pending flush for ALL caches");
            return;
        }
        if (payload.cacheName() != null && !payload.cacheName().isBlank()) {
            cacheService.evictCache(payload.cacheName());
            log.info("Successfully processed pending flush for cache: {}", payload.cacheName());
        }
    }

    private void finalizeCardProcessing(
            CharacterCard card,
            AdminCharacterCardRequest req,
            String finalName,
            String tempImagePath,
            String previousName
    ) throws IOException {

        applyCardFields(card, req);
        card.setName(finalName);
        card.setImageSrc(imageService.buildImageSrc(finalName));

        characterCardRepository.save(card);

        if (tempImagePath != null) {
            imageService.applyPendingImage(tempImagePath, finalName);
        } else if (previousName != null && !previousName.equals(finalName)) {
            imageService.renameImage(previousName, finalName);
        }
    }

    private void applyCardFields(CharacterCard card, AdminCharacterCardRequest req) {
        if (req.gender() != null && !req.gender().isBlank()) {
            card.setGender(Gender.valueOf(req.gender()));
        }
        if (req.race() != null && !req.race().isBlank()) {
            card.setRace(Race.valueOf(req.race()));
        }
        if (req.alive() != null) {
            card.setAlive(req.alive());
        }
        if (req.colors() != null) {
            card.setColors(parseEnumSet(req.colors(), Color.class));
        }
        if (req.affiliations() != null) {
            card.setAffiliations(parseEnumSet(req.affiliations(), Affiliation.class));
        }
        if (req.firstAppearanceEpisode() != null) {
            card.setFirstAppearanceEpisode(req.firstAppearanceEpisode());
        }
        if (req.aliases() != null) {
            card.setAliases(new HashSet<>(req.aliases()));
        }
    }

    private <T extends Enum<T>> Set<T> parseEnumSet(Set<String> values, Class<T> type) {
        Set<T> parsed = new HashSet<>();
        for (String value : values) {
            parsed.add(Enum.valueOf(type, value));
        }
        return parsed;
    }
}