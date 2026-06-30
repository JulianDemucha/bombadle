package com.bombadle.service.scheduling;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.service.game.*;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.stats.DailySolverStatisticService;
import com.bombadle.service.stats.PlayerStatisticsService;
import com.bombadle.service.stats.ScoreService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.admin.AdminChangeQueueService;
import com.bombadle.service.feedback.FeedbackService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "bombadle.daily-reset.enabled", havingValue = "true", matchIfMissing = true)
public class DailyResetService {
    private static final Logger log = LoggerFactory.getLogger(DailyResetService.class);

    private final CharacterCardService characterCardService;
    private final CurrentGameStateWrapper currentGameStateWrapper;
    private final ScoreService scoreService;
    private final GuessListService guessListService;
    private final CacheService cacheService;
    private final PlayerDeletionService playerDeletionService;
    private final AdminChangeQueueService adminChangeQueueService;
    private final AnonymousSessionService anonymousSessionService;
    private final AnonymousGuessListService anonymousGuessListService;
    private final CurrentCardStateService currentCardStateService;
    private final ScoreMaintenanceService scoreMaintenanceService;
    private final QuoteService quoteService;
    private final PlayerStatisticsService playerStatisticsService;
    private final DailySolverStatisticService dailySolverStatisticService;
    private final FeedbackService feedbackService;

    /* Cron:  seconds, minutes, hours, day (of the month), month, day (of the week) */
    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Warsaw")
//    @PostConstruct //for manual testing
    @Transactional
    public void executeDailyReset() {
        log.info("7:00 - Daily reset triggered: selecting new characters and resetting scores.");

        playerStatisticsService.evaluateDailyStreaks();
        dailySolverStatisticService.captureClosingDay();
        clearPreviousDayState();
        pickNewDailyEntities();
        refreshCaches();
    }

    private void clearPreviousDayState() {
        adminChangeQueueService.applyAll();
        guessListService.truncateTable();
        anonymousSessionService.truncateTable();
        anonymousGuessListService.truncateTable();
        scoreMaintenanceService.resetAllScores();
        scoreService.deleteAllInBatch();
        playerDeletionService.deleteMarkedForDeletion(Duration.ofHours(48));
        playerDeletionService.purgeExpiredDeletedAccountSnapshots(Duration.ofDays(7));
        feedbackService.deleteOlderThan(Instant.now().minus(Duration.ofDays(7)));
        log.info("All scores and previous day states have been cleared.");
    }

    private void pickNewDailyEntities() {
        Quote newQuote = quoteService.findRandomQuote();
        if (newQuote == null) {
            throw new IllegalStateException("No quotes in the database");
        }
        currentGameStateWrapper.setQuote(newQuote);

        Map<GameMode, CharacterCard> newDailyCards = new HashMap<>();

        for (GameMode mode : GameMode.values()) {
            if (mode == GameMode.QUOTES_STAGE_1) {
                continue;
            }

            CharacterCard newCard;
            if (mode == GameMode.QUOTES_STAGE_2) {
                newCard = newQuote.getCharacterCard();
            } else {
                newCard = characterCardService.findRandomCard();
            }

            if (newCard == null) {
                throw new IllegalStateException("No character cards in the database for mode: " + mode);
            }

            newDailyCards.put(mode, newCard);
            currentGameStateWrapper.set(mode, newCard);
            log.info("New Character card picked for {}: {}", mode, newCard.getName());
        }

        currentCardStateService.updateCurrentState(newDailyCards, newQuote);
    }

    private void refreshCaches() {
        cacheService.evictAllCaches();
        log.info("All caches have been evicted.");
        cacheService.reloadCardCompareCache();
        log.info("Card comparison caches have been cleared and reloaded.");
    }
}