package com.bombadle.service;

import com.bombadle.dto.CardMatcher;
import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DailyResetService {
    private static final Logger log = LoggerFactory.getLogger(DailyResetService.class);
    private final CharacterCardRepository characterCardRepository;
    private final ScoreService scoreService;
    private final CardMatcher cardMatcher;

    public CharacterCard getCurrentCard() {
        return cardMatcher.getCurrentCharacterCard();
    }

    /* Cron:  seconds, minutes, hours, day (of the month), month, day (of the week) */

    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Warsaw")
    public void pickNewCharacterCardAndResetScores() {
        log.info("7:00 - ITS SHOWTIMEEEEEE - Daily reset triggered: selecting new character and resetting scores.");
        int count;
        cardMatcher.refreshCharacterCard(characterCardRepository.findRandomCard());
        log.info("new Character card has been picked: {}", cardMatcher.getCurrentCharacterCard());
        count = scoreService.resetAllScores();
        log.info("All {} scores has been deleted", count);
    }
}
