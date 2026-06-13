package com.bombadle.service.game;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.AnonymousSessionAlreadyGuessedException;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousGuessRegistrationService {
    private final AnonymousGuessListService anonymousGuessListService;
    private final AnonymousSessionService anonymousSessionService;

    @Transactional
    public UUID registerGuessAndGetSessionId(AnonymousSession anonymousSession, GuessAttempt guessAttempt, GameMode gameMode) {
        AnonymousGuessList guessList = anonymousSession.getGuessList();
        guessList.addGuess(gameMode, guessAttempt);

        if (guessAttempt.isCorrect()){
            anonymousSession.markModeAsCompleted(gameMode);
            anonymousSession.addScoreTimestamp(gameMode, Instant.now());
        }

        anonymousSession.setGuessList(anonymousGuessListService.save(guessList));
        return anonymousSessionService.save(anonymousSession).getId();
    }

}
