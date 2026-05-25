package com.bombadle.service.scheduling;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.stats.ScoreService;
import com.bombadle.service.player.PlayerDeletionService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "bombadle.daily-reset.enabled", havingValue = "true", matchIfMissing = true)
public class DailyResetService {
    private static final Logger log = LoggerFactory.getLogger(DailyResetService.class);
    private final CharacterCardRepository characterCardRepository;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;
    private final ScoreService scoreService;
    private final GuessListService guessListService;
    private final PlayerService playerService;
    private final CacheService cacheService;
    private final PlayerDeletionService playerDeletionService;


    /* Cron:  seconds, minutes, hours, day (of the month), month, day (of the week) */
    @PostConstruct //for testing
    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Warsaw")
    @Transactional
    public void pickNewCharacterCardAndResetScores() {
        log.info("7:00 - Daily reset triggered: selecting new character and resetting scores.");
        guessListService.truncateTable();
        playerDeletionService.deleteMarkedForDeletion(Duration.ofHours(48));
        playerService.resetAllScores();
        scoreService.deleteAllInBatch();
        log.info("All scores has been deleted");
        currentCharacterCardWrapper.set(characterCardRepository.findRandomCard());
        log.info("new Character card has been picked: {}", currentCharacterCardWrapper.get().getName());
        cacheService.reloadCardCompareCache();
        log.info("Card comparison cache have been cleared and reloaded.");
    }
}
