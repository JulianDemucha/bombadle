package com.bombadle.service.game.core;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.AnonymousSessionDto;
import com.bombadle.dto.CardField;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.NameOnlyGuessAttempt;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.MatchType;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameImageServiceTest {

    @Mock
    private CurrentGameStateWrapper currentGameStateWrapper;

    @Mock
    private PlayerService playerService;

    @Mock
    private GuessListService guessListService;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @InjectMocks
    private GameImageService gameImageService;

    @Nested
    class GetCurrentImageResourceTests {

        @Test
        void getCurrentImageResource_playerLoggedNoGuesses_returnsLevelOneResource() {
            // ARRANGE
            long playerId = 1L;
            Long expectedCardId = 42L;

            Player player = mock(Player.class);
            CharacterCard card = mock(CharacterCard.class);
            GuessList guessList = mock(GuessList.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(player, GameMode.IMAGES)).thenReturn(guessList);
            when(guessList.getGuesses()).thenReturn(List.of());

            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(playerId, null);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/42/lvl_1.jpg", ((ClassPathResource) resource).getPath());
        }

        @Test
        void getCurrentImageResource_anonymousWithThreeIncorrectGuesses_returnsLevelFourResource() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            Long expectedCardId = 15L;

            CharacterCard card = mock(CharacterCard.class);
            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            NameOnlyGuessAttempt incorrectAttempt = new NameOnlyGuessAttempt(null);
            List<GuessAttempt> guesses = List.of(incorrectAttempt, incorrectAttempt, incorrectAttempt);

            AnonymousSessionDto sessionDto = new AnonymousSessionDto(
                    sessionId,
                    Map.of(GameMode.IMAGES, new com.bombadle.dto.GuessListDto(guesses)),
                    null,
                    null
            );

            when(anonymousSessionService.getAnonymousSessionReadOnly(sessionId)).thenReturn(sessionDto);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(null, sessionId);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/15/lvl_4.jpg", ((ClassPathResource) resource).getPath());
        }

        @Test
        void getCurrentImageResource_noPlayerNoSession_returnsLevelOneResource() {
            // ARRANGE
            Long expectedCardId = 99L;

            CharacterCard card = mock(CharacterCard.class);
            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(null, null);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/99/lvl_1.jpg", ((ClassPathResource) resource).getPath());
            verifyNoInteractions(playerService, anonymousSessionService, guessListService);
        }

        @Test
        void getCurrentImageResource_playerGuessedCorrectly_returnsLevelNineResource() {
            // ARRANGE
            long playerId = 1L;
            Long expectedCardId = 42L;

            Player player = mock(Player.class);
            CharacterCard card = mock(CharacterCard.class);
            GuessList guessList = mock(GuessList.class);

            @SuppressWarnings("unchecked")
            CardField<String> correctField = mock(CardField.class);
            when(correctField.match()).thenReturn(MatchType.MATCH);
            NameOnlyGuessAttempt correctAttempt = new NameOnlyGuessAttempt(correctField);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(player, GameMode.IMAGES)).thenReturn(guessList);
            when(guessList.getGuesses()).thenReturn(List.of(correctAttempt));

            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(playerId, null);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/42/lvl_9.jpg", ((ClassPathResource) resource).getPath());
        }

        @Test
        void getCurrentImageResource_anonymousTenIncorrectGuesses_maxesOutAtLevelNineResource() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            Long expectedCardId = 7L;

            CharacterCard card = mock(CharacterCard.class);
            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            NameOnlyGuessAttempt incorrectAttempt = new NameOnlyGuessAttempt(null);
            List<GuessAttempt> tenGuesses = IntStream.range(0, 10).mapToObj(i -> (GuessAttempt) incorrectAttempt).collect(Collectors.toList());

            AnonymousSessionDto sessionDto = new AnonymousSessionDto(
                    sessionId,
                    Map.of(GameMode.IMAGES, new com.bombadle.dto.GuessListDto(tenGuesses)),
                    null,
                    null
            );

            when(anonymousSessionService.getAnonymousSessionReadOnly(sessionId)).thenReturn(sessionDto);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(null, sessionId);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/7/lvl_9.jpg", ((ClassPathResource) resource).getPath());
        }

        @Test
        void getCurrentImageResource_anonymousSessionHasNoGuessLists_returnsLevelOneResource() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            Long expectedCardId = 7L;

            CharacterCard card = mock(CharacterCard.class);
            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            AnonymousSessionDto sessionDto = new AnonymousSessionDto(sessionId, null, null, null);

            when(anonymousSessionService.getAnonymousSessionReadOnly(sessionId)).thenReturn(sessionDto);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(null, sessionId);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/7/lvl_1.jpg", ((ClassPathResource) resource).getPath());
        }

        @Test
        void getCurrentImageResource_anonymousSessionHasEmptyGuessLists_returnsLevelOneResource() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            Long expectedCardId = 7L;

            CharacterCard card = mock(CharacterCard.class);
            when(currentGameStateWrapper.getCard(GameMode.IMAGES)).thenReturn(card);
            when(card.getId()).thenReturn(expectedCardId);

            AnonymousSessionDto sessionDto = new AnonymousSessionDto(
                    sessionId,
                    Map.of(GameMode.CLASSIC, new com.bombadle.dto.GuessListDto(List.of())),
                    null,
                    null
            );

            when(anonymousSessionService.getAnonymousSessionReadOnly(sessionId)).thenReturn(sessionDto);

            // ACT
            Resource resource = gameImageService.getCurrentImageResource(null, sessionId);

            // ASSERT
            assertNotNull(resource);
            assertInstanceOf(ClassPathResource.class, resource);
            assertEquals("static/images/images_mode/7/lvl_1.jpg", ((ClassPathResource) resource).getPath());
        }
    }
}