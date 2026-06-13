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
    private GameService classicGameService;

    @InjectMocks
    private GameServiceFacade gameServiceFacade;

    @Nested
    class PlayTests {

        @Test
        void play_validData_returnsGuessResponse() {
            // Arrange
            long guessCardId = 1L;
            long playerId = 2L;
            GameMode mode = GameMode.CLASSIC;
            Player player = mock(Player.class);
            CharacterCard guessCard = mock(CharacterCard.class);
            GuessResponse expectedResponse = mock(GuessResponse.class);

            when(playerService.findById(playerId)).thenReturn(Optional.of(player));
            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.of(guessCard));
            when(classicGameService.play(guessCard, player, mode)).thenReturn(expectedResponse);

            // Act
            GuessResponse result = gameServiceFacade.play(guessCardId, playerId, mode);

            // Assert
            assertEquals(expectedResponse, result);
            verify(classicGameService).play(guessCard, player, mode);
        }

        @Test
        void play_playerNotFound_throwsNoSuchElementException() {
            // Arrange
            long guessCardId = 1L;
            long playerId = 2L;
            GameMode mode = GameMode.CLASSIC;

            when(playerService.findById(playerId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(NoSuchElementException.class, () -> gameServiceFacade.play(guessCardId, playerId, mode));
            verifyNoInteractions(characterCardService, classicGameService);
        }

        @Test
        void play_cardNotFound_throwsCharacterCardNotFoundException() {
            // Arrange
            long guessCardId = 1L;
            long playerId = 2L;
            GameMode mode = GameMode.CLASSIC;
            Player player = mock(Player.class);

            when(playerService.findById(playerId)).thenReturn(Optional.of(player));
            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(CharacterCardNotFoundException.class, () -> gameServiceFacade.play(guessCardId, playerId, mode));
            verifyNoInteractions(classicGameService);
        }
    }

    @Nested
    class PlayAnonymousTests {

        @Test
        void playAnonymous_validData_returnsAnonymousGuessResponse() {
            // Arrange
            long guessCardId = 1L;
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;
            CharacterCard guessCard = mock(CharacterCard.class);
            AnonymousGuessResponse expectedResponse = mock(AnonymousGuessResponse.class);

            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.of(guessCard));
            when(classicGameService.playAnonymous(guessCard, sessionId, mode)).thenReturn(expectedResponse);

            // Act
            AnonymousGuessResponse result = gameServiceFacade.playAnonymous(guessCardId, sessionId, mode);

            // Assert
            assertEquals(expectedResponse, result);
            verify(classicGameService).playAnonymous(guessCard, sessionId, mode);
        }

        @Test
        void playAnonymous_cardNotFound_throwsCharacterCardNotFoundException() {
            // Arrange
            long guessCardId = 1L;
            UUID sessionId = UUID.randomUUID();
            GameMode mode = GameMode.CLASSIC;

            when(characterCardService.findCharacterCardById(guessCardId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(CharacterCardNotFoundException.class, () -> gameServiceFacade.playAnonymous(guessCardId, sessionId, mode));
            verifyNoInteractions(classicGameService);
        }
    }
}