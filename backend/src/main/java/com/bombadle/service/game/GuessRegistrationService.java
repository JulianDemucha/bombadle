package com.bombadle.service.game;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuessRegistrationService {
    private final GuessListService guessListService;
    private final ScoreRegistrationService scoreRegistrationService;

    @Transactional
    @CacheEvict(value = "guess-list", key = "#player.getId()+'-'+#gameMode")
    public void registerGuess(Player player, GuessAttempt guessAttempt, GameMode gameMode) {
        GuessList guessList = guessListService.findByPlayerAndGameModeOrElseCreateNew(player, gameMode);
        guessList.getGuesses().add(guessAttempt);

        if (guessAttempt.isCorrect()) {
            scoreRegistrationService.registerPlayerWin(player.getId(), guessList.getGuesses().size(), gameMode);
        }

        guessListService.save(guessList);
    }
}
