package com.bombadle.service.game.core;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.*;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.entity.Quote;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerGameStateServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private GuessListService guessListService;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @Mock
    private CurrentGameStateWrapper currentGameStateWrapper;

    @InjectMocks
    private PlayerGameStateService playerGameStateService;

    @Nested
    class GetQuotesStateForPlayerTests {

        @Test
        void getQuotesStateForPlayer_validId_returnsQuotesGameStateDto() {
            // ARRANGE
            long playerId = 1L;
            Player player = mock(Player.class);

            GuessList stageOneList = mock(GuessList.class);
            GuessList stageTwoList = mock(GuessList.class);

            QuotesStageOneAttempt s1IncorrectAttempt = new QuotesStageOneAttempt(null);

            @SuppressWarnings("unchecked")
            CardField<String> correctField = mock(CardField.class);
            when(correctField.match()).thenReturn(MatchType.MATCH);
            NameOnlyGuessAttempt s2CorrectAttempt = new NameOnlyGuessAttempt(correctField);

            when(playerService.getPlayerById(playerId)).thenReturn(player);

            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(player, GameMode.QUOTES_STAGE_1)).thenReturn(stageOneList);
            when(stageOneList.getGuesses()).thenReturn(List.of(s1IncorrectAttempt));

            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(player, GameMode.QUOTES_STAGE_2)).thenReturn(stageTwoList);
            when(stageTwoList.getGuesses()).thenReturn(List.of(s2CorrectAttempt));

            Quote currentQuote = mock(Quote.class);
            when(currentQuote.getId()).thenReturn(5L);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);

            // ACT
            QuotesGameStateDto result = playerGameStateService.getQuotesStateForPlayer(playerId);

            // ASSERT
            assertNotNull(result);
            assertEquals(1, result.stageOneGuesses().size());
            assertEquals(1, result.stageTwoGuesses().size());
            assertFalse(result.isStageOnePassed());
            assertTrue(result.isStageTwoPassed());
            assertEquals(5L, result.prompt().id());
        }

        @Test
        void getQuotesStateForPlayer_noGuessesYet_returnsDtoWithEmptyListsAndFalseBooleans() {
            // ARRANGE
            long playerId = 1L;
            Player player = mock(Player.class);

            GuessList emptyList = mock(GuessList.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(any(), any())).thenReturn(emptyList);
            when(emptyList.getGuesses()).thenReturn(List.of());

            Quote currentQuote = mock(Quote.class);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);

            // ACT
            QuotesGameStateDto result = playerGameStateService.getQuotesStateForPlayer(playerId);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.stageOneGuesses().isEmpty());
            assertTrue(result.stageTwoGuesses().isEmpty());
            assertFalse(result.isStageOnePassed());
            assertFalse(result.isStageTwoPassed());
        }
    }

    @Nested
    class GetQuotesStateForAnonymousTests {

        @Test
        void getQuotesStateForAnonymous_validSessionId_returnsQuotesGameStateDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();

            @SuppressWarnings("unchecked")
            CardField<String> correctField = mock(CardField.class);
            when(correctField.match()).thenReturn(MatchType.MATCH);
            QuotesStageOneAttempt s1CorrectAttempt = new QuotesStageOneAttempt(correctField);

            AnonymousSessionDto sessionDto = new AnonymousSessionDto(
                    sessionId,
                    Map.of(
                            GameMode.QUOTES_STAGE_1, new GuessListDto(List.of(s1CorrectAttempt)),
                            GameMode.QUOTES_STAGE_2, new GuessListDto(List.of())
                    ),
                    null,
                    null
            );

            when(anonymousSessionService.getAnonymousSessionReadOnly(sessionId)).thenReturn(sessionDto);

            Quote currentQuote = mock(Quote.class);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);

            // ACT
            QuotesGameStateDto result = playerGameStateService.getQuotesStateForAnonymous(sessionId);

            // ASSERT
            assertNotNull(result);
            assertEquals(1, result.stageOneGuesses().size());
            assertTrue(result.stageTwoGuesses().isEmpty());
            assertTrue(result.isStageOnePassed());
            assertFalse(result.isStageTwoPassed());
        }

        @Test
        void getQuotesStateForAnonymous_sessionIdIsNull_returnsEmptyQuotesGameStateDto() {
            // ARRANGE
            when(anonymousSessionService.getAnonymousSessionReadOnly(null))
                    .thenReturn(new AnonymousSessionDto(null, null, null, null));

            Quote currentQuote = mock(Quote.class);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);

            // ACT
            QuotesGameStateDto result = playerGameStateService.getQuotesStateForAnonymous(null);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.stageOneGuesses().isEmpty());
            assertTrue(result.stageTwoGuesses().isEmpty());
            assertFalse(result.isStageOnePassed());
            assertFalse(result.isStageTwoPassed());
        }

        @Test
        void getQuotesStateForAnonymous_sessionHasMissingKeys_returnsEmptyQuotesGameStateDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();

            AnonymousSessionDto sessionDto = new AnonymousSessionDto(
                    sessionId,
                    Map.of(GameMode.CLASSIC, new GuessListDto(List.of())),
                    null,
                    null
            );

            when(anonymousSessionService.getAnonymousSessionReadOnly(sessionId)).thenReturn(sessionDto);

            Quote currentQuote = mock(Quote.class);
            when(currentGameStateWrapper.getQuote()).thenReturn(currentQuote);

            // ACT
            QuotesGameStateDto result = playerGameStateService.getQuotesStateForAnonymous(sessionId);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.stageOneGuesses().isEmpty());
            assertTrue(result.stageTwoGuesses().isEmpty());
        }
    }
}