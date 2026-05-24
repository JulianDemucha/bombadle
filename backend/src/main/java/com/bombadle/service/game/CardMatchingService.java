package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.GuessResponse;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.exception.CardAlreadyGuessedException;
import com.bombadle.service.PlayerService;
import com.bombadle.service.stats.ScoreService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Getter
@Service
@RequiredArgsConstructor
public class CardMatchingService {
    private final MatchUtils matchUtils;
    private final ScoreService scoreService;
    private final PlayerService playerService;
    private final GuessListService guessListService;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;
    private final CharacterCardService characterCardService;

    @Transactional
    @CacheEvict(value = "guess-list", key = "#playerId")
    public GuessResponse compareCharacterCardClassic(Long guessCardId, long playerId) {
        Player player = playerService.findById(playerId).orElseThrow();
        CharacterCard guess = characterCardService.findCharacterCardById(guessCardId).orElseThrow();

        return compareCharacterCards(guess, currentCharacterCardWrapper.get(), player);
    }

    @Transactional
    public GuessResponse compareCharacterCards(CharacterCard guess, CharacterCard targetCharacterCard, Player player) {
        if (player.getHasGuessedToday())
            throw new CardAlreadyGuessedException();

        boolean isCorrect = false;
        GuessAttempt guessAttempt = matchUtils.compareCharacterCards(guess, targetCharacterCard);

        GuessList guessList = guessListService.findByPlayerOrElseCreateNew(player);
        guessListService.registerGuess(guessList, guessAttempt); // repo.save(guessList) included

        if (guessAttempt.isCorrect()) {
            isCorrect = true;
            Score score = scoreService.registerScore(player, guessList.getGuesses().size()); // repo.save(score) included
            playerService.registerScore(player, score); // repo.save(guessList) included
        }
        return new GuessResponse(isCorrect, guessAttempt);
    }

//    public GuessResponse compareCharacterCardClassic(CharacterCard guess, String clientIp) {
//        GuessResponse guessResponse = matchUtils.compareCharacterCardClassic(guess);
//        if (guessResponse.guessed()) {
//            //
//        }
//    }


}
