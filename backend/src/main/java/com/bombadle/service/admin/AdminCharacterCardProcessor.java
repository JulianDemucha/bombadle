package com.bombadle.service.admin;

import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.dto.request.AdminCharacterCardUpdateRequest;
import com.bombadle.dto.request.AdminQuoteRequest;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Quote;
import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Color;
import com.bombadle.enums.Gender;
import com.bombadle.enums.QuoteTarget;
import com.bombadle.enums.Race;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.repository.CurrentCardStateRepository;
import com.bombadle.repository.QuoteRepository;
import com.bombadle.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCharacterCardProcessor {

    private final CharacterCardRepository characterCardRepository;
    private final QuoteRepository quoteRepository;
    private final CurrentCardStateRepository currentCardStateRepository;
    private final CharacterCardImageService imageService;
    private final GuessImageGeneratorService guessImageGenerator;
    private final CacheService cacheService;

    @Transactional
    public void processCreate(PendingCardCreatePayload payload) {
        AdminCharacterCardRequest req = payload.card();

        if (characterCardRepository.existsByName(req.name())) {
            throw new IllegalArgumentException("Character card name already exists: " + req.name());
        }

        CharacterCard saved = saveCardFields(CharacterCard.createNewEmpty(), req, req.name());
        createQuotes(saved, req.quotes());

        Long cardId = saved.getId();
        String tempImagePath = payload.tempImagePath();
        String tempGuessImagePath = payload.tempGuessImagePath();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applyDisplayImage(tempImagePath, cardId);
                applyGuessImages(tempGuessImagePath, cardId);
            }
        });

        log.info("Successfully processed pending creation for card: {}", req.name());
    }

    @Transactional
    public void processUpdate(PendingCardUpdatePayload payload) {
        CharacterCard card = characterCardRepository.findById(payload.id())
                .orElseThrow(() -> new IllegalArgumentException("Character card not found: " + payload.id()));

        AdminCharacterCardUpdateRequest req = payload.card();
        Long cardId = card.getId();
        String nextName = req.name() != null && !req.name().isBlank() ? req.name() : card.getName();

        if (!nextName.equals(card.getName()) && characterCardRepository.existsByName(nextName)) {
            throw new IllegalArgumentException("Character card name already exists: " + nextName);
        }

        saveCardFieldsFromUpdate(card, req, nextName);

        if (req.quoteIdsToRemove() != null && !req.quoteIdsToRemove().isEmpty()) {
            // Null out any CurrentCardState references before the quotes are deleted
            removeDanglingQuoteRefsFromCurrentCardState(req.quoteIdsToRemove());
            for (Long quoteId : req.quoteIdsToRemove()) {
                quoteRepository.findById(quoteId).ifPresent(quote -> {
                    if (!quote.getCharacterCard().getId().equals(cardId)) {
                        throw new IllegalArgumentException(
                                "Quote " + quoteId + " does not belong to card " + cardId);
                    }
                    quoteRepository.delete(quote);
                });
            }
        }

        if (req.quotesToAdd() != null && !req.quotesToAdd().isEmpty()) {
            createQuotes(card, req.quotesToAdd());
        }

        String tempImagePath = payload.tempImagePath();
        String tempGuessImagePath = payload.tempGuessImagePath();

        if (tempImagePath != null || tempGuessImagePath != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (tempImagePath != null) {
                        applyDisplayImage(tempImagePath, cardId);
                    }
                    if (tempGuessImagePath != null) {
                        applyGuessImages(tempGuessImagePath, cardId);
                    }
                }
            });
        }

        log.info("Successfully processed pending update for card ID: {}", payload.id());
    }

    @Transactional
    public void processDelete(PendingCardDeletePayload payload) {
        Long cardId = payload.id();

        if (!characterCardRepository.existsById(cardId)) {
            log.warn("Attempted to delete card ID {}, but it does not exist", cardId);
            return;
        }

        // Remove FK references in CurrentCardState before cascade-deleting quotes/card
        removeDanglingRefsFromCurrentCardState(cardId);
        characterCardRepository.deleteById(cardId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteCardFiles(cardId);
            }
        });

        log.info("Successfully processed pending deletion for card ID: {}", cardId);
    }

    // ── CurrentCardState dangling-ref cleanup ─────────────────────────────────

    private void removeDanglingRefsFromCurrentCardState(Long cardId) {
        currentCardStateRepository.findById(1).ifPresent(state -> {
            boolean dirty = false;

            dirty |= state.getCurrentCards().values().removeIf(c -> c.getId().equals(cardId));
            dirty |= state.getPreviousCards().values().removeIf(c -> c.getId().equals(cardId));

            if (state.getCurrentQuote() != null
                    && state.getCurrentQuote().getCharacterCard().getId().equals(cardId)) {
                state.setCurrentQuote(null);
                dirty = true;
            }
            if (state.getPreviousQuote() != null
                    && state.getPreviousQuote().getCharacterCard().getId().equals(cardId)) {
                state.setPreviousQuote(null);
                dirty = true;
            }

            if (dirty) {
                currentCardStateRepository.save(state);
            }
        });
    }

    private void removeDanglingQuoteRefsFromCurrentCardState(List<Long> quoteIds) {
        currentCardStateRepository.findById(1).ifPresent(state -> {
            boolean dirty = false;

            if (state.getCurrentQuote() != null && quoteIds.contains(state.getCurrentQuote().getId())) {
                state.setCurrentQuote(null);
                dirty = true;
            }
            if (state.getPreviousQuote() != null && quoteIds.contains(state.getPreviousQuote().getId())) {
                state.setPreviousQuote(null);
                dirty = true;
            }

            if (dirty) {
                currentCardStateRepository.save(state);
            }
        });
    }

    // ── Shared DB helpers ─────────────────────────────────────────────────────

    private CharacterCard saveCardFields(CharacterCard card, AdminCharacterCardRequest req, String finalName) {
        applyCardFields(card, req);
        card.setName(finalName);
        // saveAndFlush guarantees the ID is assigned before we build imageSrc
        CharacterCard saved = characterCardRepository.saveAndFlush(card);
        saved.setImageSrc(imageService.buildImageSrc(saved.getId()));
        return saved;
    }

    private CharacterCard saveCardFieldsFromUpdate(CharacterCard card, AdminCharacterCardUpdateRequest req, String finalName) {
        applyCardFieldsFromUpdate(card, req);
        card.setName(finalName);
        CharacterCard saved = characterCardRepository.saveAndFlush(card);
        saved.setImageSrc(imageService.buildImageSrc(saved.getId()));
        return saved;
    }

    private void createQuotes(CharacterCard card, List<AdminQuoteRequest> quoteRequests) {
        if (quoteRequests == null || quoteRequests.isEmpty()) return;
        for (AdminQuoteRequest req : quoteRequests) {
            Quote quote = Quote.builder()
                    .characterCard(card)
                    .quoteBeginning(req.quoteBeginning())
                    .options(new ArrayList<>(req.options()))
                    .correctAnswer(req.correctAnswer())
                    .target(QuoteTarget.valueOf(req.target()))
                    .appearanceEpisode(req.appearanceEpisode())
                    .build();
            quoteRepository.save(quote);
        }
    }

    private void applyCardFieldsFromUpdate(CharacterCard card, AdminCharacterCardUpdateRequest req) {
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

    // ── afterCommit file-op helpers ───────────────────────────────────────────

    private void applyDisplayImage(String tempPath, Long cardId) {
        if (tempPath == null) return;
        try {
            imageService.scaleAndApplyDisplayImage(tempPath, cardId);
        } catch (IOException e) {
            log.error("Failed to apply display image for card {}", cardId, e);
        }
    }

    private void applyGuessImages(String tempGuessImagePath, Long cardId) {
        if (tempGuessImagePath == null) return;
        try {
            Path outputDir = imageService.getGuessImageOutputDir(cardId);
            try (InputStream in = Files.newInputStream(Paths.get(tempGuessImagePath))) {
                guessImageGenerator.generateGuessImages(in, outputDir);
            }
        } catch (IOException e) {
            log.error("Failed to generate guess images for card {}", cardId, e);
        } finally {
            try {
                imageService.deletePendingImage(tempGuessImagePath);
            } catch (IOException e) {
                log.warn("Failed to delete staged guess image for card {}", cardId, e);
            }
        }
    }

    private void deleteCardFiles(Long cardId) {
        try {
            imageService.deleteDisplayImage(cardId);
        } catch (IOException e) {
            log.error("Failed to delete display image for card {}", cardId, e);
        }
        try {
            imageService.deleteGuessImageDir(cardId);
        } catch (IOException e) {
            log.error("Failed to delete guess image directory for card {}", cardId, e);
        }
    }
}
