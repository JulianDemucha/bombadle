package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.AnonymousGuessResponse;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.GuessResponse;
import com.bombadle.entity.*;
import com.bombadle.exception.AnonymousSessionAlreadyGuessedException;
import com.bombadle.exception.CardAlreadyGuessedException;
import com.bombadle.exception.CharacterCardNotFoundException;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardMatchingServiceTest {

    @Mock
    private MatchUtils matchUtils;
    @Mock
    private ScoreService scoreService;
    @Mock
    private PlayerService playerService;
    @Mock
    private GuessListService guessListService;
    @Mock
    private CurrentCharacterCardWrapper currentCharacterCardWrapper;
    @Mock
    private CharacterCardService characterCardService;
    @Mock
    private AnonymousSessionService anonymousSessionService;
    @Mock
    private AnonymousGuessListService anonymousGuessListService;

    @InjectMocks
    private CardMatchingService cardMatchingService;

    @Nested
    class CompareCharacterCardClassicTests {

        @Test
        void compareCharacterCardClassic_playerNotFound_throwsNoSuchElementException() {
            // Arrange
            long playerId = 1L;
            Long guessCardId = 10L;
            when(playerService.findById(playerId)).thenReturn(Optional.empty());

            // Act
            Executable action = () -> cardMatchingService.compareCharacterCardClassic(guessCardId, playerId);

            // Assert
            assertThrows(NoSuchElementException.class, action);
            verifyNoInteractions(characterCardService, currentCharacterCardWrapper);
        }

        @Test
        void compareCharacterCardClassic_cardNotFound_throwsCharacterCardNotFoundException() {
            // Arrange
            long playerId = 1L;
            Long guessCardId = 10L;
            Player player = mock(Player.class);

            when(playerService.findById(playerId)).thenReturn(Optional.of(player));
            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.empty());

            // Act
            Executable action = () -> cardMatchingService.compareCharacterCardClassic(guessCardId, playerId);

            // Assert
            assertThrows(CharacterCardNotFoundException.class, action);
            verifyNoInteractions(currentCharacterCardWrapper);
        }
    }

    @Nested
    class CompareCharacterCardClassicAnonymousTests {

        @Test
        void compareCharacterCardClassicAnonymous_sessionAlreadyGuessed_throwsAnonymousSessionAlreadyGuessedException() {
            // Arrange
            Long guessCardId = 10L;
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = mock(AnonymousSession.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday()).thenReturn(true);

            // Act
            Executable action = () -> cardMatchingService.compareCharacterCardClassicAnonymous(guessCardId, sessionId);

            // Assert
            assertThrows(AnonymousSessionAlreadyGuessedException.class, action);
        }

        @Test
        void compareCharacterCardClassicAnonymous_sessionIdIsNull_createsNewSessionAndReturnsResponse() {
            // Arrange
            Long guessCardId = 10L;
            CharacterCard guessCard = mock(CharacterCard.class);
            GuessAttempt guessAttempt = mock(GuessAttempt.class);
            UUID newSessionId = UUID.randomUUID();

            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.of(guessCard));
            when(matchUtils.compareCharacterCardClassic(guessCard)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(false);
            when(anonymousGuessListService.registerGuessAndGetSessionId(any(AnonymousSession.class), eq(guessAttempt)))
                    .thenReturn(newSessionId);

            // Act
            AnonymousGuessResponse response = cardMatchingService.compareCharacterCardClassicAnonymous(guessCardId, null);

            // Assert
            assertNotNull(response);
            assertEquals(newSessionId, response.anonymousSessionId());
            assertFalse(response.guessResponse().guessAttempt().isCorrect());
            verify(anonymousGuessListService).registerGuessAndGetSessionId(any(AnonymousSession.class), eq(guessAttempt));
        }

        @Test
        void compareCharacterCardClassicAnonymous_validSessionExists_reusesSessionAndReturnsResponse() {
            // Arrange
            Long guessCardId = 10L;
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = mock(AnonymousSession.class);
            CharacterCard guessCard = mock(CharacterCard.class);
            GuessAttempt guessAttempt = mock(GuessAttempt.class);

            when(anonymousSessionService.findById(sessionId)).thenReturn(Optional.of(session));
            when(session.hasGuessedToday()).thenReturn(false);
            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.of(guessCard));
            when(matchUtils.compareCharacterCardClassic(guessCard)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(true);

            // Act
            AnonymousGuessResponse response = cardMatchingService.compareCharacterCardClassicAnonymous(guessCardId, sessionId);

            // Assert
            assertNotNull(response);
            assertEquals(sessionId, response.anonymousSessionId());
            assertTrue(response.guessResponse().guessAttempt().isCorrect());
            verify(anonymousGuessListService).registerGuess(session, guessAttempt);
        }
    }

    @Nested
    class CompareCharacterCardsTests {

        @Test
        void compareCharacterCards_playerAlreadyGuessed_throwsCardAlreadyGuessedException() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            Player player = mock(Player.class);
            when(player.getHasGuessedToday()).thenReturn(true);

            // Act
            Executable action = () -> cardMatchingService.compareCharacterCards(guess, target, player);

            // Assert
            assertThrows(CardAlreadyGuessedException.class, action);
            verifyNoInteractions(matchUtils, guessListService, scoreService);
        }

        @Test
        void compareCharacterCards_guessIsWrong_returnsIncorrectResponse() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            Player player = mock(Player.class);
            GuessAttempt guessAttempt = mock(GuessAttempt.class);
            GuessList guessList = mock(GuessList.class);

            when(player.getHasGuessedToday()).thenReturn(false);
            when(matchUtils.compareCharacterCards(guess, target)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(false);
            when(guessListService.findByPlayerOrElseCreateNew(player)).thenReturn(guessList);

            // Act
            GuessResponse response = cardMatchingService.compareCharacterCards(guess, target, player);

            // Assert
            assertNotNull(response);
            assertFalse(response.guessAttempt().isCorrect());
            assertEquals(guessAttempt, response.guessAttempt());
            verify(guessListService).registerGuess(guessList, guessAttempt);
            verifyNoInteractions(scoreService);
        }

        @Test
        void compareCharacterCards_guessIsRight_registersScoreAndReturnsCorrectResponse() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            Player player = mock(Player.class);
            GuessAttempt guessAttempt = mock(GuessAttempt.class);
            GuessList guessList = mock(GuessList.class);
            Score score = mock(Score.class);

            when(player.getHasGuessedToday()).thenReturn(false);
            when(matchUtils.compareCharacterCards(guess, target)).thenReturn(guessAttempt);
            when(guessAttempt.isCorrect()).thenReturn(true);
            when(guessListService.findByPlayerOrElseCreateNew(player)).thenReturn(guessList);
            when(guessList.getGuesses()).thenReturn(Collections.singletonList(guessAttempt));
            when(scoreService.registerScore(player, 1)).thenReturn(score);

            // Act
            GuessResponse response = cardMatchingService.compareCharacterCards(guess, target, player);

            // Assert
            assertNotNull(response);
            assertTrue(response.guessAttempt().isCorrect());
            assertEquals(guessAttempt, response.guessAttempt());
            verify(guessListService).registerGuess(guessList, guessAttempt);
            verify(scoreService).registerScore(player, 1);
            verify(playerService).registerScore(player, score);
        }
    }
}