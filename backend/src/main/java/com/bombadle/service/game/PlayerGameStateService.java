package com.bombadle.service.game;

import com.bombadle.dto.*;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerGameStateService {

    private final PlayerService playerService;
    private final GuessListService guessListService;
    private final AnonymousSessionService anonymousSessionService;
    private final GameService gameService;


    public QuotesGameStateDto getQuotesStateForPlayer(long playerId) {
        Player player = playerService.getPlayerById(playerId);

        List<GuessAttempt> rawStageOne = guessListService.findByPlayerAndGameModeOrElseCreateNew(
                player,
                GameMode.QUOTES_STAGE_1
        ).getGuesses();
        List<GuessAttempt> rawStageTwo = guessListService.findByPlayerAndGameModeOrElseCreateNew(
                player,
                GameMode.QUOTES_STAGE_2
        ).getGuesses();

        return buildQuotesStateDto(rawStageOne, rawStageTwo);
    }


    public QuotesGameStateDto getQuotesStateForAnonymous(UUID sessionId) {
        AnonymousSessionDto session = anonymousSessionService.getAnonymousSessionReadOnly(sessionId);

        List<GuessAttempt> rawStageOne = getGuessesFromSession(session, GameMode.QUOTES_STAGE_1);
        List<GuessAttempt> rawStageTwo = getGuessesFromSession(session, GameMode.QUOTES_STAGE_2);

        return buildQuotesStateDto(rawStageOne, rawStageTwo);
    }


    private QuotesGameStateDto buildQuotesStateDto(List<GuessAttempt> rawStageOne, List<GuessAttempt> rawStageTwo) {

        List<QuotesStageOneAttempt> stageOneGuesses = rawStageOne.stream()
                .filter(attempt -> attempt instanceof QuotesStageOneAttempt)
                .map(attempt -> (QuotesStageOneAttempt) attempt)
                .toList();

        List<NameOnlyGuessAttempt> stageTwoGuesses = rawStageTwo.stream()
                .filter(attempt -> attempt instanceof NameOnlyGuessAttempt)
                .map(attempt -> (NameOnlyGuessAttempt) attempt)
                .toList();

        boolean isStageOnePassed =
                !stageOneGuesses.isEmpty()
                &&
                stageOneGuesses.getLast().isCorrect();

        boolean isStageTwoPassed =
                !stageTwoGuesses.isEmpty()
                &&
                stageTwoGuesses.getLast().isCorrect();

        return new QuotesGameStateDto(
                gameService.getDailyQuotePrompt(),
                stageOneGuesses,
                stageTwoGuesses,
                isStageOnePassed,
                isStageTwoPassed
        );
    }

    private List<GuessAttempt> getGuessesFromSession(AnonymousSessionDto session, GameMode mode) {
        if (session.guessLists() == null || !session.guessLists().containsKey(mode)) {
            return Collections.emptyList();
        }
        return session.guessLists().get(mode).guessList();
    }
}