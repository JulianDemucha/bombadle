package com.bombadle.service.game;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.QuotesStageOneAttempt;
import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Player;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.AnonymousSessionAlreadyGuessedException;
import com.bombadle.exception.StageLockedException;
import com.bombadle.exception.UserAlreadyGuessedException;
import com.bombadle.service.player.AnonymousSessionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private CardMatchingService classicCardMatchingService;

    @Mock
    private QuoteMatchingService quoteMatchingService;

    @Mock
    private CurrentGameStateWrapper currentGameStateWrapper;

    @Mock
    private GuessListService guessListService;

    @Mock
    private AnonymousGuessListService anonymousGuessListService;

    @Mock
    private GuessRegistrationService guessRegistrationService;

    @Mock
    private ScoreRegistrationService scoreRegistrationService;

    @Mock
    private AnonymousGuessRegistrationService anonymousGuessRegistrationService;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @InjectMocks
    private GameService gameService;

    @Nested
    class PlayTests {

        @Test
        void play_playerAlreadyGuessed_throwsUserAlreadyGuessedException() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            Player player = mock(Player.class);
            GameMode mode = GameMode.CLASSIC;

            when(player.hasGuessedToday(mode)).thenReturn(true);

            // ACT & ASSERT
            assertThrows(UserAlreadyGuessedException.class, () -> gameService.play(guess, player, mode));
            verifyNoInteractions(classicCardMatchingService, guessRegistrationService);
        }

        @Test
        void play_quotesStage2AndStage1NotGuessed_throwsStageLockedException() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            Player player = mock(Player.class);
            GameMode mode = GameMode.QUOTES_STAGE_2;

            when(player.hasGuessedToday(mode)).thenReturn(false);
            when(player.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(false);

            // ACT & ASSERT
            assertThrows(StageLockedException.class, () -> gameService.play(guess, player, mode));
            verifyNoInteractions(classicCardMatchingService, guessRegistrationService);
        }

        @Test
        void play_validGuess_registersGuessAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            Player player = mock(Player.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.CLASSIC;

            when(player.hasGuessedToday(mode)).thenReturn(false);
            when(currentGameStateWrapper.getCard(mode)).thenReturn(target);
            when(classicCardMatchingService.compareCharacterCards(guess, target, mode)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(true);

            // ACT
            GuessResponse response = gameService.play(guess, player, mode);

            // ASSERT
            assertNotNull(response);
            assertTrue(response.guessAttempt().isCorrect());
            assertEquals(guessAttempt, response.guessAttempt());
            verify(guessRegistrationService).registerGuess(player, guessAttempt, mode);
        }

        @Test
        void play_quotesStage2AndStage1Guessed_registersGuessAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            Player player = mock(Player.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.QUOTES_STAGE_2;

            when(player.hasGuessedToday(mode)).thenReturn(false);
            when(player.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(true);
            when(currentGameStateWrapper.getCard(mode)).thenReturn(target);
            when(classicCardMatchingService.compareCharacterCards(guess, target, mode)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(false);

            // ACT
            GuessResponse response = gameService.play(guess, player, mode);

            // ASSERT
            assertNotNull(response);
            verify(guessRegistrationService).registerGuess(player, guessAttempt, mode);
        }
    }

    @Nested
    class PlayAnonymousTests {

        @Test
        void playAnonymous_sessionAlreadyGuessed_throwsAnonymousSessionAlreadyGuessedException() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;
            AnonymousSession session = mock(AnonymousSession.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday(mode)).thenReturn(true);

            // ACT & ASSERT
            assertThrows(AnonymousSessionAlreadyGuessedException.class, () -> gameService.playAnonymous(guess, sessionId, mode));
            verifyNoInteractions(classicCardMatchingService, anonymousGuessRegistrationService);
        }

        @Test
        void playAnonymous_quotesStage2AndStage1NotGuessed_throwsStageLockedException() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.QUOTES_STAGE_2;
            AnonymousSession session = mock(AnonymousSession.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday(mode)).thenReturn(false);
            when(session.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(false);

            // ACT & ASSERT
            assertThrows(StageLockedException.class, () -> gameService.playAnonymous(guess, sessionId, mode));
            verifyNoInteractions(classicCardMatchingService, anonymousGuessRegistrationService);
        }

        @Test
        void playAnonymous_sessionIdIsNull_createsNewSessionAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.CLASSIC;
            UUID newSessionId = UUID.randomUUID();

            when(currentGameStateWrapper.getCard(mode)).thenReturn(target);
            when(classicCardMatchingService.compareCharacterCards(guess, target, mode)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(false);
            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(any(AnonymousSession.class), eq(guessAttempt), eq(mode)))
                    .thenReturn(newSessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymous(guess, null, mode);

            // ASSERT
            assertNotNull(response);
            assertEquals(newSessionId, response.anonymousSessionId());
            assertFalse(response.guessResponse().guessAttempt().isCorrect());
            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(any(AnonymousSession.class), eq(guessAttempt), eq(mode));
        }

        @Test
        void playAnonymous_validSessionExists_reusesSessionAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;
            AnonymousSession session = mock(AnonymousSession.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday(mode)).thenReturn(false);
            when(currentGameStateWrapper.getCard(mode)).thenReturn(target);
            when(classicCardMatchingService.compareCharacterCards(guess, target, mode)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(true);
            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(session, guessAttempt, mode))
                    .thenReturn(sessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymous(guess, sessionId, mode);

            // ASSERT
            assertNotNull(response);
            assertEquals(sessionId, response.anonymousSessionId());
            assertTrue(response.guessResponse().guessAttempt().isCorrect());
            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(session, guessAttempt, mode);
        }

        @Test
        void playAnonymous_sessionNotFoundInDb_createsNewSessionAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            UUID invalidSessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;
            UUID newSessionId = UUID.randomUUID();

            when(anonymousSessionService.findById(invalidSessionId)).thenReturn(Optional.empty());
            when(currentGameStateWrapper.getCard(mode)).thenReturn(target);
            when(classicCardMatchingService.compareCharacterCards(guess, target, mode)).thenReturn(guessAttempt);
            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(any(AnonymousSession.class), eq(guessAttempt), eq(mode)))
                    .thenReturn(newSessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymous(guess, invalidSessionId, mode);

            // ASSERT
            assertNotNull(response);
            assertEquals(newSessionId, response.anonymousSessionId());
            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(any(AnonymousSession.class), eq(guessAttempt), eq(mode));
        }

        @Test
        void playAnonymous_quotesStage2AndStage1Guessed_registersGuessAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.QUOTES_STAGE_2;
            AnonymousSession session = mock(AnonymousSession.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday(mode)).thenReturn(false);
            when(session.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(true);
            when(currentGameStateWrapper.getCard(mode)).thenReturn(target);
            when(classicCardMatchingService.compareCharacterCards(guess, target, mode)).thenReturn(guessAttempt);
            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(session, guessAttempt, mode))
                    .thenReturn(sessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymous(guess, sessionId, mode);

            // ASSERT
            assertNotNull(response);
            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(session, guessAttempt, mode);
        }
    }

    @Nested
    class PlayQuotesStageOneTests {

        @Test
        void playQuotesStageOne_playerAlreadyGuessed_throwsUserAlreadyGuessedException() {
            // ARRANGE
            String guess = "Some quote option";
            Player player = mock(Player.class);

            when(player.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(true);

            // ACT & ASSERT
            assertThrows(UserAlreadyGuessedException.class, () -> gameService.playQuotesStageOne(guess, player));
            verifyNoInteractions(quoteMatchingService, guessRegistrationService);
        }

        @Test
        void playQuotesStageOne_validGuess_registersGuessAndReturnsResponse() {
            // ARRANGE
            String guess = "Some quote option";
            Player player = mock(Player.class);
            Quote currentQuote = mock(Quote.class);
            QuotesStageOneAttempt attempt = mock(QuotesStageOneAttempt.class);

            when(player.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(false);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);
            when(quoteMatchingService.guess(guess, currentQuote)).thenReturn(attempt);
            when(attempt.isCorrect()).thenReturn(true);

            // ACT
            GuessResponse response = gameService.playQuotesStageOne(guess, player);

            // ASSERT
            assertNotNull(response);
            assertTrue(response.guessAttempt().isCorrect());
            verify(guessRegistrationService).registerGuess(player, attempt, GameMode.QUOTES_STAGE_1);
        }

        @Test
        void playAnonymousQuotesStageOne_sessionNotFoundInDb_createsNewSessionAndReturnsResponse() {
            // ARRANGE
            String guess = "Option";
            UUID invalidSessionId = UUID.randomUUID();
            Quote currentQuote = mock(Quote.class);
            QuotesStageOneAttempt attempt = mock(QuotesStageOneAttempt.class);
            UUID newSessionId = UUID.randomUUID();

            when(anonymousSessionService.findById(invalidSessionId)).thenReturn(Optional.empty());
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);
            when(quoteMatchingService.guess(guess, currentQuote)).thenReturn(attempt);
            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(any(AnonymousSession.class), eq(attempt), eq(GameMode.QUOTES_STAGE_1)))
                    .thenReturn(newSessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymousQuotesStageOne(guess, invalidSessionId);

            // ASSERT
            assertNotNull(response);
            assertEquals(newSessionId, response.anonymousSessionId());
            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(any(AnonymousSession.class), eq(attempt), eq(GameMode.QUOTES_STAGE_1));
        }
    }

    @Nested
    class PlayAnonymousQuotesStageOneTests {

        @Test
        void playAnonymousQuotesStageOne_sessionAlreadyGuessed_throwsAnonymousSessionAlreadyGuessedException() {
            // ARRANGE
            String guess = "Option";
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = mock(AnonymousSession.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(true);

            // ACT & ASSERT
            assertThrows(AnonymousSessionAlreadyGuessedException.class, () -> gameService.playAnonymousQuotesStageOne(guess, sessionId));
            verifyNoInteractions(quoteMatchingService, anonymousGuessRegistrationService);
        }

        @Test
        void playAnonymousQuotesStageOne_validSession_registersGuessAndReturnsResponse() {
            // ARRANGE
            String guess = "Option";
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = mock(AnonymousSession.class);
            Quote currentQuote = mock(Quote.class);
            QuotesStageOneAttempt attempt = mock(QuotesStageOneAttempt.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday(GameMode.QUOTES_STAGE_1)).thenReturn(false);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);
            when(quoteMatchingService.guess(guess, currentQuote)).thenReturn(attempt);
            when(attempt.isCorrect()).thenReturn(true);

            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(session, attempt, GameMode.QUOTES_STAGE_1))
                    .thenReturn(sessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymousQuotesStageOne(guess, sessionId);

            // ASSERT
            assertNotNull(response);
            assertEquals(sessionId, response.anonymousSessionId());
            assertTrue(response.guessResponse().guessAttempt().isCorrect());
            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(session, attempt, GameMode.QUOTES_STAGE_1);
        }

        @Test
        void playAnonymousQuotesStageOne_sessionIdIsNull_createsNewSessionAndReturnsResponse() {
            // ARRANGE
            String guess = "Option";
            Quote currentQuote = mock(Quote.class);
            QuotesStageOneAttempt attempt = mock(QuotesStageOneAttempt.class);
            UUID newSessionId = UUID.randomUUID();

            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);
            when(quoteMatchingService.guess(guess, currentQuote)).thenReturn(attempt);
            when(attempt.isCorrect()).thenReturn(false);

            when(anonymousGuessRegistrationService.registerGuessAndGetSessionId(any(AnonymousSession.class), eq(attempt), eq(GameMode.QUOTES_STAGE_1)))
                    .thenReturn(newSessionId);

            // ACT
            AnonymousGuessResponse response = gameService.playAnonymousQuotesStageOne(guess, null);

            // ASSERT
            assertNotNull(response);
            assertEquals(newSessionId, response.anonymousSessionId());

            verify(anonymousGuessRegistrationService).registerGuessAndGetSessionId(any(AnonymousSession.class), eq(attempt), eq(GameMode.QUOTES_STAGE_1));

            verifyNoInteractions(anonymousSessionService);
        }
    }
}