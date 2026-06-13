package com.bombadle.service.scheduling;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.GameMode;
import com.bombadle.service.game.*;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.stats.ScoreService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.admin.AdminChangeQueueService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "bombadle.daily-reset.enabled", havingValue = "true", matchIfMissing = true)
public class DailyResetService {
    private static final Logger log = LoggerFactory.getLogger(DailyResetService.class);
    private final CharacterCardService characterCardService;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;
    private final ScoreService scoreService;
    private final GuessListService guessListService;
    private final PlayerService playerService;
    private final CacheService cacheService;
    private final PlayerDeletionService playerDeletionService;
    private final AdminChangeQueueService adminChangeQueueService;
    private final AnonymousSessionService anonymousSessionService;
    private final AnonymousGuessListService anonymousGuessListService;
    private final CurrentCardStateService currentCardStateService;
    private final ScoreMaintenanceService scoreMaintenanceService;

    /* Cron:  seconds, minutes, hours, day (of the month), month, day (of the week) */
    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Warsaw")
//    @PostConstruct //for manual testing
    @Transactional
    public void pickNewCharacterCardAndResetScores() {
        log.info("7:00 - Daily reset triggered: selecting new characters and resetting scores.");

        adminChangeQueueService.applyAll();

        guessListService.truncateTable();
        anonymousSessionService.truncateTable();
        anonymousGuessListService.truncateTable();
        scoreMaintenanceService.resetAllScores();
        scoreService.deleteAllInBatch();
        playerDeletionService.deleteMarkedForDeletion(Duration.ofHours(48));
        log.info("All scores have been deleted");

        Map<GameMode, CharacterCard> newDailyCards = new HashMap<>();

        for (GameMode mode : GameMode.values()) {
            CharacterCard newCard = characterCardService.findRandomCard();
            if (newCard == null) {
                throw new IllegalStateException("No character cards in the database");
            }
            newDailyCards.put(mode, newCard);

            currentCharacterCardWrapper.set(mode, newCard);
            log.info("New Character card picked for {}: {}", mode, newCard.getName());
        }
        currentCardStateService.updateCurrentCards(newDailyCards);

        cacheService.reloadCardCompareCache();
        log.info("Card comparison caches have been cleared and reloaded.");
    }
}
