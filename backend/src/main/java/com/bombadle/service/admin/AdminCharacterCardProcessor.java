package com.bombadle.service.admin;

import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.dto.request.AdminQuoteRequest;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Quote;
import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Color;
import com.bombadle.enums.Gender;
import com.bombadle.enums.QuoteTarget;
import com.bombadle.enums.Race;
import com.bombadle.repository.CharacterCardRepository;
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
                applyDisplayImage(tempImagePath, cardId, true);
                applyGuessImages(tempGuessImagePath, cardId);
            }
        });

        log.info("Successfully processed pending creation for card: {}", req.name());
    }

    @Transactional
    public void processUpdate(PendingCardUpdatePayload payload) {
        CharacterCard card = characterCardRepository.findById(payload.id())
                .orElseThrow(() -> new IllegalArgumentException("Character card not found: " + payload.id()));

        AdminCharacterCardRequest req = payload.card();
        String nextName = req.name() != null && !req.name().isBlank() ? req.name() : card.getName();

        if (!nextName.equals(card.getName()) && characterCardRepository.existsByName(nextName)) {
            throw new IllegalArgumentException("Character card name already exists: " + nextName);
        }

        Long cardId = card.getId();
        saveCardFields(card, req, nextName);

        String tempImagePath = payload.tempImagePath();
        if (tempImagePath != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Simple move — Stage 4 will upgrade this to scaleAndApply
                    applyDisplayImage(tempImagePath, cardId, false);
                }
            });
        }

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

    // ── Shared DB helpers ─────────────────────────────────────────────────────

    private CharacterCard saveCardFields(CharacterCard card, AdminCharacterCardRequest req, String finalName) {
        applyCardFields(card, req);
        card.setName(finalName);
        // saveAndFlush guarantees the ID is assigned before we build imageSrc
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

    private void applyDisplayImage(String tempPath, Long cardId, boolean scale) {
        if (tempPath == null) return;
        try {
            if (scale) {
                imageService.scaleAndApplyDisplayImage(tempPath, cardId);
            } else {
                imageService.applyPendingImage(tempPath, cardId);
            }
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
}
