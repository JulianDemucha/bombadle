package com.bombadle.service.game;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.AnonymousSessionDto;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.NameOnlyGuessAttempt;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameImageService {

    private final CurrentGameStateWrapper currentGameStateWrapper;
    private final PlayerService playerService;
    private final GuessListService guessListService;
    private final AnonymousSessionService anonymousSessionService;

    public Resource getCurrentImageResource(Long playerId, UUID anonymousSessionId) {
        int allowedLevel = calculateAllowedLevel(playerId, anonymousSessionId);
        Long currentCardId = currentGameStateWrapper.getCard(GameMode.IMAGES).getId();

        String internalPath = "static/images/images_mode/" + currentCardId + "/lvl_" + allowedLevel + ".jpg";
        return new ClassPathResource(internalPath);
    }

    private int calculateAllowedLevel(Long playerId, UUID anonymousSessionId) {
        List<GuessAttempt> guesses;

        if (playerId != null) {
            Player player = playerService.getPlayerById(playerId);
            guesses = guessListService.findByPlayerAndGameModeOrElseCreateNew(player, GameMode.IMAGES).getGuesses();
        } else if (anonymousSessionId != null) {
            AnonymousSessionDto session = anonymousSessionService.getAnonymousSessionReadOnly(anonymousSessionId);
            guesses = getGuessesFromSession(session);
        } else {
            return 1;
        }

        boolean isGuessed = guesses.stream()
                .filter(attempt -> attempt instanceof NameOnlyGuessAttempt)
                .map(attempt -> (NameOnlyGuessAttempt) attempt)
                .anyMatch(NameOnlyGuessAttempt::isCorrect);

        if (isGuessed) return 10;
        return Math.min(guesses.size() + 1, 10);
    }

    private List<GuessAttempt> getGuessesFromSession(AnonymousSessionDto session) {
        if (session.guessLists() == null || !session.guessLists().containsKey(GameMode.IMAGES)) {
            return Collections.emptyList();
        }
        return session.guessLists().get(GameMode.IMAGES).guessList();
    }
}