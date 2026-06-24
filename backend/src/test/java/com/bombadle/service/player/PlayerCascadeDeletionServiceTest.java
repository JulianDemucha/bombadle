package com.bombadle.service.player;

import com.bombadle.entity.Player;
import com.bombadle.service.auth.cookie.RefreshTokenService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.stats.PlayerStatisticsService;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerCascadeDeletionServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private GuessListService guessListService;

    @Mock
    private ScoreService scoreService;

    @Mock
    private PlayerStatisticsService playerStatisticsService;

    @InjectMocks
    private PlayerCascadeDeletionService playerCascadeDeletionService;

    @Nested
    class DeletePlayerWithCascadeTests {

        @Test
        void deletePlayerWithCascade_validPlayer_deletesRelatedDataAndPlayer() {
            // Arrange
            Player player = mock(Player.class);
            Long playerId = 10L;
            when(player.getId()).thenReturn(playerId);

            // Act
            playerCascadeDeletionService.deletePlayerWithCascade(player);

            // Assert
            verify(guessListService).deleteAllByPlayerId(playerId);
            verify(scoreService).deleteAllByPlayerId(playerId);
            verify(playerStatisticsService).deleteAllByPlayerId(playerId);
            verify(refreshTokenService).deleteAllByPlayerId(playerId);
            verify(playerService).manualDelete(player);
        }
    }
}