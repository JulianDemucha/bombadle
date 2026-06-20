package com.bombadle.service.game;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.QuotePromptDto;
import com.bombadle.dto.QuotesStageOneAttempt;
import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.entity.*;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.AnonymousSessionAlreadyGuessedException;
import com.bombadle.exception.StageLockedException;
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
    private final QuoteMatchingService quoteMatchingService;
    private final CurrentGameStateWrapper currentGameStateWrapper;

    private final GuessListService guessListService;
    private final AnonymousGuessListService anonymousGuessListService;

    private final GuessRegistrationService guessRegistrationService;
    private final ScoreRegistrationService scoreRegistrationService;
    private final AnonymousGuessRegistrationService anonymousGuessRegistrationService;

    private final AnonymousSessionService anonymousSessionService;


    // ----- CLASSIC, QUOTE_STAGE_2, IMAGES -----

    @Transactional
    public GuessResponse play(CharacterCard guess, Player player, GameMode gameMode) {

        if (player.hasGuessedToday(gameMode))
            throw new UserAlreadyGuessedException();

        if (gameMode.equals(GameMode.QUOTES_STAGE_2) && !player.hasGuessedToday(GameMode.QUOTES_STAGE_1))
            throw new StageLockedException("You must complete Quotes Stage 1 before playing Stage 2.");

        GuessAttempt guessAttempt = classicCardMatchingService.compareCharacterCards(
                guess,
                currentGameStateWrapper.getCard(gameMode),
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

        // get session if not null and exists - else create new empty session
        if (anonymousSessionId != null) {
            session = anonymousSessionService
                    .findById(anonymousSessionId)
                    .orElse(AnonymousSession.createEmptySession());

            if (session.hasGuessedToday(gameMode)) {
                throw new AnonymousSessionAlreadyGuessedException();
            }

            if (gameMode.equals(GameMode.QUOTES_STAGE_2) && !session.hasGuessedToday(GameMode.QUOTES_STAGE_1))
                throw new StageLockedException("You must complete Quotes Stage 1 before playing Stage 2.");

        } else {
            session = AnonymousSession.createEmptySession();
        }

        GuessAttempt guessAttempt =
                classicCardMatchingService.compareCharacterCards(
                        guess,
                        currentGameStateWrapper.getCard(gameMode),
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


    // ----- QUOTE STAGE 1 -----

    @Transactional
    public GuessResponse playQuotesStageOne(String guess, Player player) {
        if (player.hasGuessedToday(GameMode.QUOTES_STAGE_1))
            throw new UserAlreadyGuessedException();

        QuotesStageOneAttempt guessAttempt = quoteMatchingService.guess(
                guess,
                currentGameStateWrapper.getQuote()
        );

        guessRegistrationService.registerGuess(
                player,
                guessAttempt,
                GameMode.QUOTES_STAGE_1
        );
        return new GuessResponse(guessAttempt.isCorrect(), guessAttempt);
    }

    @Transactional
    public AnonymousGuessResponse playAnonymousQuotesStageOne(
            String guess,
            UUID anonymousSessionId
    ) {
        AnonymousSession session;

        // get session if not null and exists - else create new empty session
        if (anonymousSessionId != null) {
            session = anonymousSessionService
                    .findById(anonymousSessionId)
                    .orElse(AnonymousSession.createEmptySession());

            if (session.hasGuessedToday(GameMode.QUOTES_STAGE_1))
                throw new AnonymousSessionAlreadyGuessedException();

        } else {
            session = AnonymousSession.createEmptySession();
        }

        GuessAttempt guessAttempt =
                quoteMatchingService.guess(
                        guess,
                        currentGameStateWrapper.getQuote()
                );

        anonymousSessionId =
                anonymousGuessRegistrationService.registerGuessAndGetSessionId(
                        session,
                        guessAttempt,
                        GameMode.QUOTES_STAGE_1
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

    // ----- TODAY'S QUOTE -----

    public QuotePromptDto getDailyQuotePrompt() {
        Quote currentQuote = currentGameStateWrapper.getQuote();
        return new QuotePromptDto(
                currentQuote.getId(),
                currentQuote.getQuoteBeginning(),
                currentQuote.getOptions(),
                currentQuote.getAppearanceEpisode(),
                currentQuote.getTarget()
        );
    }


}
