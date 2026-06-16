package com.bombadle.service.game;

import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.CharacterCardNotFoundException;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceFacadeTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private CharacterCardService characterCardService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameServiceFacade gameServiceFacade;

    @Nested
    class PlayTests {

        @Test
        void play_validData_returnsGuessResponse() {
            // ARRANGE
            long guessCardId = 1L;
            long playerId = 2L;
            GameMode mode = GameMode.CLASSIC;
            Player player = mock(Player.class);
            CharacterCard guessCard = mock(CharacterCard.class);
            GuessResponse expectedResponse = mock(GuessResponse.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.of(guessCard));
            when(gameService.play(guessCard, player, mode)).thenReturn(expectedResponse);

            // ACT
            GuessResponse result = gameServiceFacade.play(guessCardId, playerId, mode);

            // ASSERT
            assertEquals(expectedResponse, result);
            verify(gameService).play(guessCard, player, mode);
        }

        @Test
        void play_playerNotFound_throwsException() {
            // ARRANGE
            long guessCardId = 1L;
            long playerId = 2L;
            GameMode mode = GameMode.CLASSIC;

            when(playerService.getPlayerById(playerId)).thenThrow(new NoSuchElementException());

            // ACT & ASSERT
            assertThrows(NoSuchElementException.class, () -> gameServiceFacade.play(guessCardId, playerId, mode));
            verifyNoInteractions(characterCardService, gameService);
        }

        @Test
        void play_cardNotFound_throwsCharacterCardNotFoundException() {
            // ARRANGE
            long guessCardId = 1L;
            long playerId = 2L;
            GameMode mode = GameMode.CLASSIC;
            Player player = mock(Player.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.empty());

            // ACT & ASSERT
            assertThrows(CharacterCardNotFoundException.class, () -> gameServiceFacade.play(guessCardId, playerId, mode));
            verifyNoInteractions(gameService);
        }
    }

    @Nested
    class PlayAnonymousTests {

        @Test
        void playAnonymous_validData_returnsAnonymousGuessResponse() {
            // ARRANGE
            long guessCardId = 1L;
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;
            CharacterCard guessCard = mock(CharacterCard.class);
            AnonymousGuessResponse expectedResponse = mock(AnonymousGuessResponse.class);

            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.of(guessCard));
            when(gameService.playAnonymous(guessCard, sessionId, mode)).thenReturn(expectedResponse);

            // ACT
            AnonymousGuessResponse result = gameServiceFacade.playAnonymous(guessCardId, sessionId, mode);

            // ASSERT
            assertEquals(expectedResponse, result);
            verify(gameService).playAnonymous(guessCard, sessionId, mode);
        }

        @Test
        void playAnonymous_cardNotFound_throwsCharacterCardNotFoundException() {
            // ARRANGE
            long guessCardId = 1L;
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;

            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.empty());

            // ACT & ASSERT
            assertThrows(CharacterCardNotFoundException.class, () -> gameServiceFacade.playAnonymous(guessCardId, sessionId, mode));
            verifyNoInteractions(gameService);
        }
    }

    @Nested
    class PlayQuotesStageOneTests {

        @Test
        void playQuotesStageOne_validData_returnsGuessResponse() {
            // ARRANGE
            String guess = "Quote option";
            long playerId = 2L;
            Player player = mock(Player.class);
            GuessResponse expectedResponse = mock(GuessResponse.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(gameService.playQuotesStageOne(guess, player)).thenReturn(expectedResponse);

            // ACT
            GuessResponse result = gameServiceFacade.playQuotesStageOne(guess, playerId);

            // ASSERT
            assertEquals(expectedResponse, result);
            verify(gameService).playQuotesStageOne(guess, player);
        }

        @Test
        void playQuotesStageOne_playerNotFound_throwsException() {
            // ARRANGE
            String guess = "Quote option";
            long playerId = 2L;

            when(playerService.getPlayerById(playerId)).thenThrow(new NoSuchElementException());

            // ACT & ASSERT
            assertThrows(NoSuchElementException.class, () -> gameServiceFacade.playQuotesStageOne(guess, playerId));
            verifyNoInteractions(gameService);
        }
    }

    @Nested
    class PlayAnonymousQuotesStageOneTests {

        @Test
        void playAnonymousQuotesStageOne_validData_returnsAnonymousGuessResponse() {
            // ARRANGE
            String guess = "Quote option";
            UUID sessionId = UUID.randomUUID();
            AnonymousGuessResponse expectedResponse = mock(AnonymousGuessResponse.class);

            when(gameService.playAnonymousQuotesStageOne(guess, sessionId)).thenReturn(expectedResponse);

            // ACT
            AnonymousGuessResponse result = gameServiceFacade.playAnonymousQuotesStageOne(guess, sessionId);

            // ASSERT
            assertEquals(expectedResponse, result);
            verify(gameService).playAnonymousQuotesStageOne(guess, sessionId);
        }
    }
}