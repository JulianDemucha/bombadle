package com.bombadle.service.game;

import com.bombadle.repository.PlayerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScoreMaintenanceServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private ScoreMaintenanceService scoreMaintenanceService;

    @Nested
    class ResetAllScoresTests {

        @Test
        void resetAllScores_called_callsRepositoryMethods() {
            // Arrange
            // Act
            scoreMaintenanceService.resetAllScores();

            // Assert
            verify(playerRepository).resetAllScores();
            verify(playerRepository).flush();
        }
    }
}