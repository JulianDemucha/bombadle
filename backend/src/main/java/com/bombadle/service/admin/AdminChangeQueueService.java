package com.bombadle.service.admin;

import com.bombadle.dto.AdminPendingCardChangeDto;
import com.bombadle.entity.AdminPendingChange;
import com.bombadle.repository.AdminPendingChangeRepository;
import com.bombadle.dto.queue.PendingCacheFlushPayload;
import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChangeQueueService {

    private final AdminPendingChangeRepository pendingChangeRepository;
    private final AdminCharacterCardProcessor processor;
    private final CharacterCardImageService imageService;
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
            processor.processCreate(payload);
            return;
        }
        if (actionType.startsWith("update_card")) {
            PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
            processor.processUpdate(payload);
            return;
        }
        if (actionType.startsWith("delete_card")) {
            PendingCardDeletePayload payload = objectMapper.readValue(change.getPayload(), PendingCardDeletePayload.class);
            processor.processDelete(payload);
            return;
        }
        if (actionType.startsWith("flush_cache")) {
            PendingCacheFlushPayload payload = objectMapper.readValue(change.getPayload(), PendingCacheFlushPayload.class);
            processor.processCacheFlush(payload);
        }
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

    public List<AdminPendingCardChangeDto> listPendingCardChanges() {
        List<AdminPendingChange> changes = pendingChangeRepository.findAllByOrderByCreatedAtAsc();
        List<AdminPendingCardChangeDto> result = new ArrayList<>();
        for (AdminPendingChange change : changes) {
            try {
                String actionType = change.getActionType();
                if (actionType.startsWith("create_card")) {
                    PendingCardCreatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardCreatePayload.class);
                    String name = payload.card() != null ? payload.card().name() : null;
                    result.add(new AdminPendingCardChangeDto(
                            actionType,
                            "create",
                            null,
                            name,
                            change.getCreatedAt()
                    ));
                } else if (actionType.startsWith("update_card")) {
                    PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
                    String name = payload.card() != null ? payload.card().name() : null;
                    result.add(new AdminPendingCardChangeDto(
                            actionType,
                            "update",
                            payload.id(),
                            name,
                            change.getCreatedAt()
                    ));
                } else if (actionType.startsWith("delete_card")) {
                    PendingCardDeletePayload payload = objectMapper.readValue(change.getPayload(), PendingCardDeletePayload.class);
                    result.add(new AdminPendingCardChangeDto(
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