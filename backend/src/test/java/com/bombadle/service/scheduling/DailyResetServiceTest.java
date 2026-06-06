package com.bombadle.service.scheduling;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.service.admin.AdminChangeQueueService;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.game.AnonymousGuessListService;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.CurrentCardStateService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @InjectMocks
    private DailyResetService dailyResetService;

    @Nested
    class PickNewCharacterCardAndResetScoresTests {

        @Test
        void pickNewCharacterCardAndResetScores_standardTrigger_executesAllResetStepsInSequence() {
            // Arrange
            CharacterCard mockCard = mock(CharacterCard.class);
            when(mockCard.getName()).thenReturn("Gandalf");
            when(characterCardService.findRandomCard()).thenReturn(mockCard);
            when(currentCharacterCardWrapper.get()).thenReturn(mockCard);

            // Act
            dailyResetService.pickNewCharacterCardAndResetScores();

            // Assert
            InOrder inOrder = inOrder(
                    adminChangeQueueService,
                    guessListService,
                    anonymousSessionService,
                    anonymousGuessListService,
                    playerService,
                    scoreService,
                    playerDeletionService,
                    characterCardService,
                    currentCharacterCardWrapper,
                    currentCardStateService,
                    cacheService
            );

            inOrder.verify(adminChangeQueueService).applyAll();
            inOrder.verify(guessListService).truncateTable();
            inOrder.verify(anonymousSessionService).truncateTable();
            inOrder.verify(anonymousGuessListService).truncateTable();
            inOrder.verify(playerService).resetAllScores();
            inOrder.verify(scoreService).deleteAllInBatch();
            inOrder.verify(playerDeletionService).deleteMarkedForDeletion(Duration.ofHours(48));
            inOrder.verify(characterCardService).findRandomCard();
            inOrder.verify(currentCharacterCardWrapper).set(mockCard);
            inOrder.verify(currentCardStateService).updateCurrentCard(mockCard);
            inOrder.verify(currentCharacterCardWrapper).get();
            inOrder.verify(cacheService).reloadCardCompareCache();
        }

        @Test
        void pickNewCharacterCardAndResetScores_serviceThrowsException_propagatesExceptionAndInterruptsFlow() {
            // Arrange
            doThrow(new RuntimeException("Database error"))
                    .when(characterCardService).findRandomCard();

            // Act & Assert
            assertThrows(RuntimeException.class, () -> dailyResetService.pickNewCharacterCardAndResetScores());

            verify(adminChangeQueueService).applyAll();
            verify(guessListService).truncateTable();
            verify(playerDeletionService).deleteMarkedForDeletion(Duration.ofHours(48));

            verify(currentCharacterCardWrapper, never()).set(any());
            verify(currentCardStateService, never()).updateCurrentCard(any());
            verifyNoInteractions(cacheService);
        }
    }
}