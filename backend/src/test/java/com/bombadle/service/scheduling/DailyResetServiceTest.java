package com.bombadle.service.scheduling;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.GameMode;
import com.bombadle.service.admin.AdminChangeQueueService;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.game.AnonymousGuessListService;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.CurrentCardStateService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.game.ScoreMaintenanceService;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.player.PlayerService;
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
    private CurrentCharacterCardWrapper currentCharacterCardWrapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private GuessListService guessListService;
    @Mock
    private PlayerService playerService;
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

    @InjectMocks
    private DailyResetService dailyResetService;

    @Nested
    class PickNewCharacterCardAndResetScoresTests {

        @Test
        void pickNewCharacterCardAndResetScores_validState_executesResetFlowAndUpdatesCards() {
            // Arrange
            CharacterCard mockCard = mock(CharacterCard.class);
            when(characterCardService.findRandomCard()).thenReturn(mockCard);

            // Act
            dailyResetService.pickNewCharacterCardAndResetScores();

            // Assert
            verify(adminChangeQueueService).applyAll();
            verify(guessListService).truncateTable();
            verify(anonymousSessionService).truncateTable();
            verify(anonymousGuessListService).truncateTable();
            verify(scoreMaintenanceService).resetAllScores();
            verify(scoreService).deleteAllInBatch();
            verify(playerDeletionService).deleteMarkedForDeletion(any(Duration.class));

            verify(characterCardService, times(GameMode.values().length)).findRandomCard();
            verify(currentCharacterCardWrapper, times(GameMode.values().length)).set(any(GameMode.class), eq(mockCard));
            verify(currentCardStateService).updateCurrentCards(any());
            verify(cacheService).reloadCardCompareCache();
        }

        @Test
        void pickNewCharacterCardAndResetScores_noCardsInDatabase_throwsIllegalStateException() {
            // Arrange
            when(characterCardService.findRandomCard()).thenReturn(null);

            // Act
            // Assert
            assertThrows(IllegalStateException.class, () -> dailyResetService.pickNewCharacterCardAndResetScores());

            verify(adminChangeQueueService).applyAll();
            verify(guessListService).truncateTable();
            verify(anonymousSessionService).truncateTable();
            verify(anonymousGuessListService).truncateTable();
            verify(scoreMaintenanceService).resetAllScores();
            verify(scoreService).deleteAllInBatch();
            verify(playerDeletionService).deleteMarkedForDeletion(any(Duration.class));

            verify(currentCharacterCardWrapper, never()).set(any(), any());
            verify(currentCardStateService, never()).updateCurrentCards(any());
            verifyNoInteractions(cacheService);
        }
    }
}