package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.entity.AdminPendingChange;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Color;
import com.bombadle.enums.Gender;
import com.bombadle.enums.Race;
import com.bombadle.repository.AdminPendingChangeRepository;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.admin.queue.PendingCacheFlushPayload;
import com.bombadle.service.admin.queue.PendingCardCreatePayload;
import com.bombadle.service.admin.queue.PendingCardDeletePayload;
import com.bombadle.service.admin.queue.PendingCardUpdatePayload;
import com.bombadle.service.cache.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminChangeQueueService {
    private static final Logger log = LoggerFactory.getLogger(AdminChangeQueueService.class);

    private final AdminPendingChangeRepository pendingChangeRepository;
    private final CharacterCardRepository characterCardRepository;
    private final CharacterCardImageService imageService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public void enqueue(String actionType, Object payload) {
        enqueue(actionType, null, payload);
    }

    public void enqueue(String actionType, String actionKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (actionKey != null && !actionKey.isBlank()) {
                var existing = pendingChangeRepository.findFirstByActionKey(actionKey);
                if (existing.isPresent()) {
                    AdminPendingChange change = existing.get();
                    change.setActionType(actionType);
                    change.setPayload(json);
                    change.setCreatedAt(Instant.now());
                    pendingChangeRepository.save(change);
                    return;
                }
            }
            AdminPendingChange change = AdminPendingChange.builder()
                    .actionType(actionType)
                    .actionKey(actionKey)
                    .payload(json)
                    .createdAt(Instant.now())
                    .build();
            pendingChangeRepository.save(change);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize pending change payload", e);
        }
    }

    public void applyAll() {
        List<AdminPendingChange> changes = pendingChangeRepository.findAllByOrderByCreatedAtAsc();
        for (AdminPendingChange change : changes) {
            try {
                applyChange(change);
            } catch (Exception e) {
                log.error("Failed to apply pending change {}", change.getId(), e);
            } finally {
                pendingChangeRepository.delete(change);
            }
        }
    }

    private void applyChange(AdminPendingChange change) throws IOException {
        String actionType = change.getActionType();
        if (actionType.startsWith("create_card")) {
            PendingCardCreatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardCreatePayload.class);
            applyCreateCard(payload);
            return;
        }
        if (actionType.startsWith("update_card")) {
            PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
            applyUpdateCard(payload);
            return;
        }
        if (actionType.startsWith("delete_card")) {
            PendingCardDeletePayload payload = objectMapper.readValue(change.getPayload(), PendingCardDeletePayload.class);
            characterCardRepository.deleteById(payload.id());
            return;
        }
        if (actionType.startsWith("flush_cache")) {
            PendingCacheFlushPayload payload = objectMapper.readValue(change.getPayload(), PendingCacheFlushPayload.class);
            applyCacheFlush(payload);
        }
    }

    private void applyCreateCard(PendingCardCreatePayload payload) throws IOException {
        AdminCharacterCardRequest req = payload.card();
        if (characterCardRepository.existsByName(req.name())) {
            throw new IllegalArgumentException("Character card name already exists: " + req.name());
        }
        CharacterCard card = CharacterCard.create();
        applyCardFields(card, req);
        card.setImageSrc(imageService.buildImageSrc(req.name()));
        characterCardRepository.save(card);
        imageService.applyPendingImage(payload.tempImagePath(), req.name());
    }

    private void applyUpdateCard(PendingCardUpdatePayload payload) throws IOException {
        CharacterCard card = characterCardRepository.findById(payload.id())
                .orElseThrow(() -> new IllegalArgumentException("Character card not found: " + payload.id()));
        AdminCharacterCardRequest req = payload.card();
        String previousName = payload.previousName();
        String nextName = req.name() != null && !req.name().isBlank() ? req.name() : card.getName();

        if (!nextName.equals(card.getName()) && characterCardRepository.existsByName(nextName)) {
            throw new IllegalArgumentException("Character card name already exists: " + nextName);
        }

        applyCardFields(card, req);
        card.setName(nextName);
        card.setImageSrc(imageService.buildImageSrc(nextName));
        characterCardRepository.save(card);

        if (payload.tempImagePath() != null) {
            imageService.applyPendingImage(payload.tempImagePath(), nextName);
        } else if (previousName != null && !previousName.equals(nextName)) {
            imageService.renameImage(previousName, nextName);
        }
    }

    private void applyCacheFlush(PendingCacheFlushPayload payload) {
        if (Boolean.TRUE.equals(payload.flushAll())) {
            cacheService.evictAllCaches();
            return;
        }
        if (payload.cacheName() != null && !payload.cacheName().isBlank()) {
            cacheService.evictCache(payload.cacheName());
        }
    }

    private void applyCardFields(CharacterCard card, AdminCharacterCardRequest req) {
        if (req.name() != null && !req.name().isBlank()) {
            card.setName(req.name());
        }
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

    public boolean hasPendingActionKey(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return false;
        }
        return pendingChangeRepository.findFirstByActionKey(actionKey).isPresent();
    }

    public boolean hasPendingCardName(String name, Long excludeCardId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String targetSlug = imageService.buildSlug(name);
        List<AdminPendingChange> changes = pendingChangeRepository.findAll();
        for (AdminPendingChange change : changes) {
            try {
                String actionType = change.getActionType();
                if (actionType.startsWith("create_card")) {
                    PendingCardCreatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardCreatePayload.class);
                    if (payload.card() != null && payload.card().name() != null) {
                        String slug = imageService.buildSlug(payload.card().name());
                        if (targetSlug.equals(slug)) {
                            return true;
                        }
                    }
                } else if (actionType.startsWith("update_card")) {
                    PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
                    String candidate = payload.card() != null ? payload.card().name() : null;
                    if (candidate != null && !candidate.isBlank()) {
                        if (excludeCardId != null && excludeCardId.equals(payload.id())) {
                            continue;
                        }
                        String slug = imageService.buildSlug(candidate);
                        if (targetSlug.equals(slug)) {
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to parse pending change {} for name check", change.getId(), e);
            }
        }
        return false;
    }

    public boolean cancelByActionKey(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return false;
        }
        var existing = pendingChangeRepository.findFirstByActionKey(actionKey);
        if (existing.isEmpty()) {
            return false;
        }
        AdminPendingChange change = existing.get();
        try {
            cleanupPendingPayload(change);
        } catch (IOException e) {
            log.warn("Failed to cleanup pending payload for change {}", change.getId(), e);
        }
        pendingChangeRepository.delete(change);
        return true;
    }

    private void cleanupPendingPayload(AdminPendingChange change) throws IOException {
        String actionType = change.getActionType();
        if (actionType.startsWith("create_card")) {
            PendingCardCreatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardCreatePayload.class);
            imageService.deletePendingImage(payload.tempImagePath());
        } else if (actionType.startsWith("update_card")) {
            PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
            imageService.deletePendingImage(payload.tempImagePath());
        }
    }

    public java.util.List<com.bombadle.dto.AdminPendingCardChangeDto> listPendingCardChanges() {
        java.util.List<AdminPendingChange> changes = pendingChangeRepository.findAllByOrderByCreatedAtAsc();
        java.util.List<com.bombadle.dto.AdminPendingCardChangeDto> result = new java.util.ArrayList<>();
        for (AdminPendingChange change : changes) {
            try {
                String actionType = change.getActionType();
                if (actionType.startsWith("create_card")) {
                    PendingCardCreatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardCreatePayload.class);
                    String name = payload.card() != null ? payload.card().name() : null;
                    result.add(new com.bombadle.dto.AdminPendingCardChangeDto(
                            actionType,
                            "create",
                            null,
                            name,
                            change.getCreatedAt()
                    ));
                } else if (actionType.startsWith("update_card")) {
                    PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
                    String name = payload.card() != null ? payload.card().name() : null;
                    result.add(new com.bombadle.dto.AdminPendingCardChangeDto(
                            actionType,
                            "update",
                            payload.id(),
                            name,
                            change.getCreatedAt()
                    ));
                } else if (actionType.startsWith("delete_card")) {
                    PendingCardDeletePayload payload = objectMapper.readValue(change.getPayload(), PendingCardDeletePayload.class);
                    result.add(new com.bombadle.dto.AdminPendingCardChangeDto(
                            actionType,
                            "delete",
                            payload.id(),
                            null,
                            change.getCreatedAt()
                    ));
                }
            } catch (IOException e) {
                log.warn("Failed to parse pending change {} for list", change.getId(), e);
            }
        }
        return result;
    }
}

