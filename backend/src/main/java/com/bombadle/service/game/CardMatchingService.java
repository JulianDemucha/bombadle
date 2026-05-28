package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.AnonymousGuessResponse;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.GuessResponse;
import com.bombadle.entity.*;
import com.bombadle.exception.CardAlreadyGuessedException;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.ScoreService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

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
    private final AnonymousSessionService anonymousSessionService;
    private final AnonymousGuessListService anonymousGuessListService;

    @Transactional
    @CacheEvict(value = "guess-list", key = "#playerId")
    public GuessResponse compareCharacterCardClassic(Long guessCardId, long playerId) {
        Player player = playerService.findById(playerId).orElseThrow();
        CharacterCard guess = characterCardService.findCharacterCardById(guessCardId).orElseThrow();

        return compareCharacterCards(guess, currentCharacterCardWrapper.get(), player);
    }

    @Transactional
    public AnonymousGuessResponse compareCharacterCardClassicAnonymous(Long guessCardId, UUID anonymousSessionId) {
        AnonymousSession session = null;

        if (anonymousSessionId != null) {
            session = anonymousSessionService.findById(anonymousSessionId).orElse(null);

            if (session != null && session.hasGuessedToday()) {
                throw new RuntimeException(); //todo custom exception
            }
        }

        CharacterCard guess = characterCardService.findCharacterCardById(guessCardId).orElseThrow(); //todo custom exception

        GuessAttempt guessAttempt = matchUtils.compareCharacterCardClassic(guess);

        if (session == null) {
            anonymousSessionId = anonymousGuessListService.registerGuessAndGetSessionId(
                    new AnonymousSession(new AnonymousGuessList()),
                    guessAttempt
            );
        } else {
            anonymousGuessListService.registerGuess(session, guessAttempt);
        }

        return AnonymousGuessResponse.builder()
                .anonymousSessionId(anonymousSessionId)
                .guessResponse(
                        GuessResponse.builder()
                                .correct(guessAttempt.isCorrect())
                                .guessAttempt(guessAttempt)
                                .build()
                )
                .build();
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


}
