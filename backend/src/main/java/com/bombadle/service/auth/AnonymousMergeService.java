package com.bombadle.service.auth;

import com.bombadle.entity.AnonymousSession;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.service.game.AnonymousGuessListService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousMergeService {
    private final AnonymousSessionService anonymousSessionService;
    private final ScoreService scoreService;
    private final PlayerService playerService;
    private final GuessListService guessListService;
    private final AnonymousGuessListService anonymousGuessListService;

    @Transactional
    public void handleAnonymousSessionMerge(Player player, UUID anonymousSessionId, Boolean triggerMerge) {
        if (anonymousSessionId == null ||  isNullOrFalse(triggerMerge)) {
            return;
        }

        Optional<AnonymousSession> sessionOpt = anonymousSessionService.findById(anonymousSessionId);

        if (sessionOpt.isEmpty()) {
            return;
        }

        AnonymousSession session = sessionOpt.get();

        if (player.getHasGuessedToday() || !session.hasGuessedToday()) {
            return;
        }

        // save score with original timestamp from the anonymous session
        Score score = scoreService.registerScoreWithTimestamp(player, session.getGuessList().getGuesses().size(), session.getScoreTimestamp());

        // create a new guess list for the player and copy the attempts
        GuessList newGuessList = new GuessList(player);
        newGuessList.getGuesses().addAll(session.getGuessList().getGuesses());
        guessListService.manualSave(newGuessList);

        // update player's state
        playerService.registerScore(player, score);

        anonymousGuessListService.delete(session.getGuessList());
        anonymousSessionService.delete(session);
    }

    private boolean isNullOrFalse(Boolean b){
        return !Boolean.TRUE.equals(b);
    }
}
