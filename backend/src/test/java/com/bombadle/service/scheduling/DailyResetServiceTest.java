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
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

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

    @InjectMocks
    private DailyResetService dailyResetService;

    @Nested
    class PickNewCharacterCardAndResetScoresTests {

        @Test
        void executeDailyReset_validState_executesResetFlowAndUpdatesCards() {
            // Arrange
            CharacterCard mockRandomCard = mock(CharacterCard.class);
            when(mockRandomCard.getName()).thenReturn("Random Name");

            CharacterCard mockQuoteCard = mock(CharacterCard.class);
            when(mockQuoteCard.getName()).thenReturn("Quote Character Name");

            Quote mockQuote = mock(Quote.class);
            when(mockQuote.getCharacterCard()).thenReturn(mockQuoteCard);

            when(quoteService.findRandomQuote()).thenReturn(mockQuote);
            when(characterCardService.findRandomCard()).thenReturn(mockRandomCard);

            // Act
            dailyResetService.executeDailyReset();

            // Assert
            verify(adminChangeQueueService).applyAll();
            verify(guessListService).truncateTable();
            verify(anonymousSessionService).truncateTable();
            verify(anonymousGuessListService).truncateTable();
            verify(scoreMaintenanceService).resetAllScores();
            verify(scoreService).deleteAllInBatch();
            verify(playerDeletionService).deleteMarkedForDeletion(any(Duration.class));

            verify(quoteService).findRandomQuote();
            verify(currentGameStateWrapper).setQuote(mockQuote);

            verify(characterCardService, times(2)).findRandomCard();

            verify(currentGameStateWrapper).set(GameMode.CLASSIC, mockRandomCard);
            verify(currentGameStateWrapper).set(GameMode.IMAGES, mockRandomCard);
            verify(currentGameStateWrapper).set(GameMode.QUOTES_STAGE_2, mockQuoteCard);

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
            verify(characterCardService, never()).findRandomCard();
            verify(currentGameStateWrapper, never()).set(any(), any());
            verify(currentCardStateService, never()).updateCurrentState(any(), any());
        }

        @Test
        void executeDailyReset_noCardsInDatabase_throwsIllegalStateException() {
            // Arrange
            Quote mockQuote = mock(Quote.class);
            when(quoteService.findRandomQuote()).thenReturn(mockQuote);

            when(characterCardService.findRandomCard()).thenReturn(null);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> dailyResetService.executeDailyReset());

            verify(adminChangeQueueService).applyAll();
            verify(currentGameStateWrapper).setQuote(mockQuote);
            verify(currentCardStateService, never()).updateCurrentState(any(), any());
            verifyNoInteractions(cacheService);
        }
    }
}