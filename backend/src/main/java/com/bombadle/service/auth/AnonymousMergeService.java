package com.bombadle.service.auth;

import com.bombadle.entity.AnonymousSession;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
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

        if (session.getGuessLists() == null || session.getGuessLists().isEmpty()) {
            anonymousSessionService.delete(session);
            return;
        }

        session.getGuessLists().forEach(anonGuessList -> {
            GameMode gameMode = anonGuessList.getGameMode();
            var attempts = anonGuessList.getGuesses();

            if (player.hasGuessedToday(gameMode)) {
                return;
            }

            if (attempts == null || attempts.isEmpty()) {
                return;
            }

            boolean isCorrect = attempts.getLast().isCorrect();

            GuessList newGuessList = GuessList.builder()
                    .guesses(attempts)
                    .gameMode(gameMode)
                    .player(player)
                    .build();
            guessListService.save(newGuessList);

            if (isCorrect) {
                scoreRegistrationService.registerPlayerWinWithTimestamp(
                        player.getId(),
                        attempts.size(),
                        gameMode,
                        session.getScoreTimestamps().get(gameMode)
                );
            }
        });

        anonymousSessionService.delete(session);
    }

    private boolean isNullOrFalse(Boolean b) {
        return !Boolean.TRUE.equals(b);
    }
}