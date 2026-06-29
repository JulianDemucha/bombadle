package com.bombadle.service.admin;

import com.bombadle.dto.AdminPendingCardChangeDto;
import com.bombadle.entity.AdminPendingChange;
import com.bombadle.repository.AdminPendingChangeRepository;
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

    private final AdminPendingChangeRepository repo;
    private final AdminCharacterCardProcessor processor;
    private final CharacterCardImageService imageService;
    private final ObjectMapper objectMapper;


    /**
     * Enqueues a task without a deduplication key (actionKey = null).
     * Use this method for actions that can be safely duplicated in the queue
     * and do not require checking for existing pending tasks.
     */
    public void enqueue(String actionType, Object payload) {
        enqueue(actionType, null, payload);
    }

    public void enqueue(String actionType, String actionKey, Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Queue payload cannot be null for action: " + actionType);
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (actionKey != null && !actionKey.isBlank()) {
                var existing = repo.findFirstByActionKey(actionKey);
                if (existing.isPresent()) {
                    AdminPendingChange change = existing.get();
                    change.setActionType(actionType);
                    change.setPayload(json);
                    change.setCreatedAt(Instant.now());
                    repo.save(change);
                    return;
                }
            }
            AdminPendingChange change = AdminPendingChange.builder()
                    .actionType(actionType)
                    .actionKey(actionKey)
                    .payload(json)
                    .createdAt(Instant.now())
                    .build();
            repo.save(change);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize pending change payload", e);
        }
    }

    public void applyAll() {
        List<AdminPendingChange> changes = repo.findAllByOrderByCreatedAtAsc();
        for (AdminPendingChange change : changes) {
            try {
                applyChange(change);
            } catch (Exception e) {
                log.error("Failed to apply pending change {}", change.getId(), e);
            } finally {
                repo.delete(change);
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
        }
    }

    public boolean hasPendingActionKey(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return false;
        }
        return repo.findFirstByActionKey(actionKey).isPresent();
    }

    public boolean hasPendingCardName(String name, Long excludeCardId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String targetSlug = imageService.buildSlug(name);
        List<AdminPendingChange> changes = repo.findAll();
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
        var existing = repo.findFirstByActionKey(actionKey);
        if (existing.isEmpty()) {
            return false;
        }
        AdminPendingChange change = existing.get();
        try {
            cleanupPendingPayload(change);
        } catch (IOException e) {
            log.warn("Failed to cleanup pending payload for change {}", change.getId(), e);
        }
        repo.delete(change);
        return true;
    }

    private void cleanupPendingPayload(AdminPendingChange change) throws IOException {
        String actionType = change.getActionType();
        if (actionType.startsWith("create_card")) {
            PendingCardCreatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardCreatePayload.class);
            imageService.deletePendingImage(payload.tempImagePath());
            imageService.deletePendingImage(payload.tempGuessImagePath());
        } else if (actionType.startsWith("update_card")) {
            PendingCardUpdatePayload payload = objectMapper.readValue(change.getPayload(), PendingCardUpdatePayload.class);
            imageService.deletePendingImage(payload.tempImagePath());
            imageService.deletePendingImage(payload.tempGuessImagePath());
        }
    }

    public List<AdminPendingCardChangeDto> listPendingCardChanges() {
        List<AdminPendingChange> changes = repo.findAllByOrderByCreatedAtAsc();
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