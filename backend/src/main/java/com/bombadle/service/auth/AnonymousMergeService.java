package com.bombadle.service.auth;

import com.bombadle.entity.AnonymousSession;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.service.game.AnonymousGuessListService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.game.ScoreRegistrationService;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousMergeService {
    private final AnonymousSessionService anonymousSessionService;
    private final GuessListService guessListService;
    private final AnonymousGuessListService anonymousGuessListService;
    private final ScoreRegistrationService scoreRegistrationService;

    @Transactional
    public void handleAnonymousSessionMerge(Player player, UUID anonymousSessionId, Boolean triggerMerge) {
        if (anonymousSessionId == null || isNullOrFalse(triggerMerge)) {
            return;
        }

        Optional<AnonymousSession> sessionOpt = anonymousSessionService.findById(anonymousSessionId);
        if (sessionOpt.isEmpty()) {
            return;
        }

        AnonymousSession session = sessionOpt.get();

        if (session.getGuessList() == null || session.getGuessList().getGuesses() == null || session.getGuessList().getGuesses().isEmpty()) {
            return;
        }

        session.getGuessList().getGuesses().forEach((gameMode, attempts) -> {

            if (player.hasGuessedToday(gameMode)) {
                return;
            }

            if (attempts.isEmpty()) {
                return;
            }

            boolean isCorrect = attempts.getLast().isCorrect();

            GuessList newGuessList = GuessList.builder()
                    .guesses(attempts)
                    .gameMode(gameMode)
                    .build();
            guessListService.save(newGuessList);

            if (isCorrect) {
                scoreRegistrationService.registerPlayerWin(player, attempts.size(), gameMode);
            }
        });

        anonymousGuessListService.delete(session.getGuessList());
        anonymousSessionService.delete(session);
    }

    private boolean isNullOrFalse(Boolean b) {
        return !Boolean.TRUE.equals(b);
    }
}