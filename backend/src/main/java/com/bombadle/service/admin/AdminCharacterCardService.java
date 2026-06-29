package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.dto.request.AdminQuoteRequest;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCharacterCardService {
    private final CharacterCardRepository characterCardRepository;
    private final AdminAuditService adminAuditService;
    private final AdminChangeQueueService changeQueueService;
    private final CharacterCardImageService imageService;

    public void enqueueCreate(long actorId, AdminCharacterCardRequest request, MultipartFile image, MultipartFile guessImage) throws IOException {
        validateCreate(request);
        if (characterCardRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Character card name already exists: " + request.name());
        }
        if (changeQueueService.hasPendingCardName(request.name(), null)) {
            throw new IllegalArgumentException("Character card name already pending: " + request.name());
        }
        String slug = imageService.buildSlug(request.name());
        String actionKey = "card:create:" + slug;
        if (changeQueueService.hasPendingActionKey(actionKey)) {
            throw new IllegalArgumentException("Character card creation already pending: " + request.name());
        }
        String tempImagePath = imageService.storePendingImage(image);
        String tempGuessImagePath = imageService.storePendingGuessImage(guessImage);
        changeQueueService.enqueue("create_card", actionKey,
                new PendingCardCreatePayload(request, tempImagePath, tempGuessImagePath));
        adminAuditService.logAction(actorId, "create_card_pending", request.name());
    }

    public void enqueueUpdate(long actorId, long id, AdminCharacterCardRequest request, MultipartFile image, String currentName) throws IOException {
        if (request == null) {
            throw new AdminOperationNotAllowedException("Update payload is required");
        }
        if (request.name() != null && !request.name().isBlank() && !request.name().equals(currentName)
                && characterCardRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Character card name already exists: " + request.name());
        }
        if (request.name() != null && !request.name().isBlank() && !request.name().equals(currentName)
                && changeQueueService.hasPendingCardName(request.name(), id)) {
            throw new IllegalArgumentException("Character card name already pending: " + request.name());
        }
        String tempImagePath = image != null && !image.isEmpty() ? imageService.storePendingImage(image) : null;
        String actionKey = "card:" + id;
        changeQueueService.enqueue("update_card_" + id, actionKey, new PendingCardUpdatePayload(id, request, tempImagePath));
        adminAuditService.logAction(actorId, "update_card_" + id, null);
    }

    public void enqueueDelete(long actorId, long id) {
        String actionKey = "card:" + id;
        changeQueueService.enqueue("delete_card_" + id, actionKey, new PendingCardDeletePayload(id));
        adminAuditService.logAction(actorId, "delete_card_" + id, null);
    }

    public void cancelCreate(long actorId, String name) {
        String slug = imageService.buildSlug(name);
        String actionKey = "card:create:" + slug;
        boolean removed = changeQueueService.cancelByActionKey(actionKey);
        if (!removed) {
            throw new IllegalArgumentException("Pending create not found for: " + name);
        }
        adminAuditService.logAction(actorId, "cancel_create_card_" + slug, null);
    }

    public void cancelUpdate(long actorId, long id) {
        String actionKey = "card:" + id;
        boolean removed = changeQueueService.cancelByActionKey(actionKey);
        if (!removed) {
            throw new IllegalArgumentException("Pending change not found for card: " + id);
        }
        adminAuditService.logAction(actorId, "cancel_update_card_" + id, null);
    }

    public void cancelDelete(long actorId, long id) {
        String actionKey = "card:" + id;
        boolean removed = changeQueueService.cancelByActionKey(actionKey);
        if (!removed) {
            throw new IllegalArgumentException("Pending change not found for card: " + id);
        }
        adminAuditService.logAction(actorId, "cancel_delete_card_" + id, null);
    }

    public List<com.bombadle.dto.AdminPendingCardChangeDto> listPendingChanges() {
        return changeQueueService.listPendingCardChanges();
    }

    private void validateCreate(AdminCharacterCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.gender() == null || request.gender().isBlank()) {
            throw new IllegalArgumentException("Gender is required");
        }
        if (request.quotes() == null || request.quotes().isEmpty()) {
            throw new IllegalArgumentException("At least one quote is required");
        }
        for (AdminQuoteRequest quote : request.quotes()) {
            validateQuote(quote);
        }
    }

    private void validateQuote(AdminQuoteRequest quote) {
        if (quote.quoteBeginning() == null || quote.quoteBeginning().isBlank()) {
            throw new IllegalArgumentException("Quote beginning is required");
        }
        if (quote.options() == null || quote.options().isEmpty()) {
            throw new IllegalArgumentException("Quote options are required");
        }
        if (quote.correctAnswer() == null || quote.correctAnswer().isBlank()) {
            throw new IllegalArgumentException("Quote correct answer is required");
        }
        if (!quote.options().contains(quote.correctAnswer())) {
            throw new IllegalArgumentException("Correct answer must be one of the options: " + quote.correctAnswer());
        }
        if (quote.target() == null || quote.target().isBlank()) {
            throw new IllegalArgumentException("Quote target is required");
        }
        if (quote.appearanceEpisode() == null) {
            throw new IllegalArgumentException("Quote appearance episode is required");
        }
    }
}
