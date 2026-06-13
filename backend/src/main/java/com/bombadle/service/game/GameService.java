package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.entity.*;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.AnonymousSessionAlreadyGuessedException;
import com.bombadle.exception.UserAlreadyGuessedException;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Getter
@Service
@RequiredArgsConstructor
public class GameService {
    private final CardMatchingService classicCardMatchingService;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;

    private final GuessListService guessListService;
    private final ScoreRegistrationService scoreRegistrationService;

    private final AnonymousGuessListService anonymousGuessListService;
    private final AnonymousSessionService anonymousSessionService;
    private final AnonymousGuessRegistrationService anonymousGuessRegistrationService;
    private final GuessRegistrationService guessRegistrationService;

    @Transactional
    public GuessResponse play(CharacterCard guess, Player player, GameMode gameMode) {

        if(player.hasGuessedToday(gameMode))
            throw new UserAlreadyGuessedException();

        GuessAttempt guessAttempt = classicCardMatchingService.compareCharacterCards(
                guess,
                currentCharacterCardWrapper.get(gameMode),
                gameMode
        );

        guessRegistrationService.registerGuess(
                player,
                guessAttempt,
                gameMode
        );

        return new GuessResponse(guessAttempt.isCorrect(), guessAttempt);
    }

    @Transactional
    public AnonymousGuessResponse playAnonymous(
            CharacterCard guess,
            UUID anonymousSessionId,
            GameMode gameMode
    ) {
        AnonymousSession session;

        // get session if not null and exists, else create new empty session
        if (anonymousSessionId != null) {
            session = anonymousSessionService
                    .findById(anonymousSessionId)
                    .orElse(AnonymousSession.createEmptySession());

            if (session.hasGuessedToday(gameMode)) {
                throw new AnonymousSessionAlreadyGuessedException();
            }

        } else {
            session = AnonymousSession.createEmptySession();
        }

        GuessAttempt guessAttempt =
                classicCardMatchingService.compareCharacterCards(
                        guess,
                        currentCharacterCardWrapper.get(gameMode),
                        gameMode
                );

        anonymousSessionId =
                anonymousGuessRegistrationService.registerGuessAndGetSessionId(
                        session,
                        guessAttempt,
                        gameMode
                );

        return AnonymousGuessResponse.builder()
                .anonymousSessionId(anonymousSessionId)
                .guessResponse(
                        new GuessResponse(
                                guessAttempt.isCorrect(),
                                guessAttempt
                        )
                ).build();
    }


}
