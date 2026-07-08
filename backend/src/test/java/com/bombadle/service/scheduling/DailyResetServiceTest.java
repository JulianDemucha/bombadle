package com.bombadle.service.scheduling;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.service.admin.AdminChangeQueueService;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.game.*;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.feedback.FeedbackService;
import com.bombadle.service.stats.DailySolverStatisticService;
import com.bombadle.service.stats.PlayerStatisticsService;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyResetServiceTest {

    @Mock
    private CharacterCardService characterCardService;
    @Mock
    private CurrentGameStateWrapper currentGameStateWrapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private GuessListService guessListService;
    @Mock
    private CacheService cacheService;
    @Mock
    private PlayerDeletionService playerDeletionService;
    @Mock
    private AdminChangeQueueService adminChangeQueueService;
    @Mock
    private AnonymousSessionService anonymousSessionService;
    @Mock
    private AnonymousGuessListService anonymousGuessListService;
    @Mock
    private CurrentCardStateService currentCardStateService;
    @Mock
    private ScoreMaintenanceService scoreMaintenanceService;
    @Mock
    private QuoteService quoteService;
    @Mock
    private PlayerStatisticsService playerStatisticsService;
    @Mock
    private DailySolverStatisticService dailySolverStatisticService;
    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private DailyResetService dailyResetService;

    @Nested
    class PickNewCharacterCardAndResetScoresTests {

        @Test
        void executeDailyReset_validState_executesResetFlowAndUpdatesCardsExcludingPreviouslyPicked() {
            // Arrange
            CharacterCard mockQuoteCard = mock(CharacterCard.class);
            when(mockQuoteCard.getId()).thenReturn(100L);
            when(mockQuoteCard.getName()).thenReturn("Quote Character Name");

            CharacterCard mockClassicCard = mock(CharacterCard.class);
            when(mockClassicCard.getId()).thenReturn(200L);
            when(mockClassicCard.getName()).thenReturn("Classic Name");

            CharacterCard mockImagesCard = mock(CharacterCard.class);
            when(mockImagesCard.getId()).thenReturn(300L);
            when(mockImagesCard.getName()).thenReturn("Images Name");

            Quote mockQuote = mock(Quote.class);
            when(mockQuote.getCharacterCard()).thenReturn(mockQuoteCard);

            when(quoteService.findRandomQuote()).thenReturn(mockQuote);

            // The production code reuses one mutable exclusion list across calls, so it must be
            // defensively copied at call time (an ArgumentCaptor would only ever see its final state).
            List<List<Long>> exclusionsPerCall = new ArrayList<>();
            when(characterCardService.findRandomCardExcluding(any())).thenAnswer(invocation -> {
                List<Long> excludedIds = invocation.getArgument(0);
                exclusionsPerCall.add(List.copyOf(excludedIds));
                return exclusionsPerCall.size() == 1 ? mockClassicCard : mockImagesCard;
            });

            // Act
            dailyResetService.executeDailyReset();

            // Assert
            verify(playerStatisticsService).evaluateDailyStreaks();
            verify(dailySolverStatisticService).captureClosingDay();
            verify(adminChangeQueueService).applyAll();
            verify(guessListService).truncateTable();
            verify(anonymousSessionService).truncateTable();
            verify(anonymousGuessListService).truncateTable();
            verify(scoreMaintenanceService).resetAllScores();
            verify(scoreService).deleteAllInBatch();
            verify(playerDeletionService).deleteMarkedForDeletion(any(Duration.class));
            verify(playerDeletionService).purgeExpiredDeletedAccountSnapshots(any(Duration.class));

            // Streaks must be evaluated, and the end-of-day solver totals captured, from
            // completedModesToday BEFORE the reset wipes it.
            InOrder inOrder = inOrder(playerStatisticsService, dailySolverStatisticService, scoreMaintenanceService);
            inOrder.verify(playerStatisticsService).evaluateDailyStreaks();
            inOrder.verify(dailySolverStatisticService).captureClosingDay();
            inOrder.verify(scoreMaintenanceService).resetAllScores();

            verify(quoteService).findRandomQuote();
            verify(currentGameStateWrapper).setQuote(mockQuote);

            // CLASSIC is picked excluding only the quote's card; IMAGES is picked excluding both
            // the quote's card and the just-picked CLASSIC card, guaranteeing the three are distinct.
            verify(characterCardService, times(2)).findRandomCardExcluding(any());
            assertEquals(2, exclusionsPerCall.size());
            assertEquals(List.of(100L), exclusionsPerCall.get(0));
            assertEquals(List.of(100L, 200L), exclusionsPerCall.get(1));

            verify(currentGameStateWrapper).set(GameMode.QUOTES_STAGE_2, mockQuoteCard);
            verify(currentGameStateWrapper).set(GameMode.CLASSIC, mockClassicCard);
            verify(currentGameStateWrapper).set(GameMode.IMAGES, mockImagesCard);

            verify(currentCardStateService).updateCurrentState(any(), eq(mockQuote));
            verify(cacheService).reloadCardCompareCache();
        }

        @Test
        void executeDailyReset_noQuotesInDatabase_throwsIllegalStateException() {
            // Arrange
            when(quoteService.findRandomQuote()).thenReturn(null);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> dailyResetService.executeDailyReset());

            verify(adminChangeQueueService).applyAll();
            verify(scoreService).deleteAllInBatch(); // Wszystko co jest przed wylosowaniem się wykonuje
            verify(characterCardService, never()).findRandomCardExcluding(any());
            verify(currentGameStateWrapper, never()).set(any(), any());
            verify(currentCardStateService, never()).updateCurrentState(any(), any());
        }

        @Test
        void executeDailyReset_quoteHasNoCharacterCard_throwsIllegalStateException() {
            // Arrange
            Quote mockQuote = mock(Quote.class);
            when(mockQuote.getCharacterCard()).thenReturn(null);
            when(quoteService.findRandomQuote()).thenReturn(mockQuote);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> dailyResetService.executeDailyReset());

            verify(currentGameStateWrapper).setQuote(mockQuote);
            verify(characterCardService, never()).findRandomCardExcluding(any());
            verify(currentCardStateService, never()).updateCurrentState(any(), any());
        }

        @Test
        void executeDailyReset_noCardsInDatabase_throwsIllegalStateException() {
            // Arrange
            CharacterCard mockQuoteCard = mock(CharacterCard.class);
            when(mockQuoteCard.getId()).thenReturn(100L);

            Quote mockQuote = mock(Quote.class);
            when(mockQuote.getCharacterCard()).thenReturn(mockQuoteCard);
            when(quoteService.findRandomQuote()).thenReturn(mockQuote);

            when(characterCardService.findRandomCardExcluding(any())).thenReturn(null);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> dailyResetService.executeDailyReset());

            verify(adminChangeQueueService).applyAll();
            verify(currentGameStateWrapper).setQuote(mockQuote);
            verify(currentCardStateService, never()).updateCurrentState(any(), any());
            verifyNoInteractions(cacheService);
        }
    }
}