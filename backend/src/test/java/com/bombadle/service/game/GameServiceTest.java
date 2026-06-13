package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.AnonymousSessionAlreadyGuessedException;
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
    private CurrentCharacterCardWrapper currentCharacterCardWrapper;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @Mock
    private AnonymousGuessRegistrationService anonymousGuessRegistrationService;

    @Mock
    private GuessRegistrationService guessRegistrationService;

    @InjectMocks
    private GameService gameService;

    @Nested
    class PlayTests {

        @Test
        void play_playerAlreadyGuessed_throwsException() {
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
        void play_validGuess_registersGuessAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            Player player = mock(Player.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.CLASSIC;

            when(player.hasGuessedToday(mode)).thenReturn(false);
            when(currentCharacterCardWrapper.get(mode)).thenReturn(target);
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
    }

    @Nested
    class PlayAnonymousTests {

        @Test
        void playAnonymous_sessionAlreadyGuessed_throwsException() {
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
        void playAnonymous_sessionIdIsNull_createsNewSessionAndReturnsResponse() {
            // ARRANGE
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            ClassicGuessAttempt guessAttempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.CLASSIC;
            UUID newSessionId = UUID.randomUUID();

            when(currentCharacterCardWrapper.get(mode)).thenReturn(target);
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
            when(currentCharacterCardWrapper.get(mode)).thenReturn(target);
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
    }
}
