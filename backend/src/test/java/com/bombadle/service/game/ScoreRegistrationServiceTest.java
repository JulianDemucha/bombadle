package com.bombadle.service.game;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.LeaderboardService;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreRegistrationServiceTest {

    @Mock
    private ScoreService scoreService;

    @Mock
    private PlayerService playerService;

    @Mock
    private CacheService cacheService;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private ScoreRegistrationService scoreRegistrationService;

    @Captor
    private ArgumentCaptor<Score> scoreCaptor;

    @Nested
    class RegisterPlayerWinTests {

        @Test
        void registerPlayerWin_validData_savesScoreUpdatesPlayerAndClearsCaches() {
            // Arrange
            Player player = mock(Player.class);
            int numberOfTries = 3;
            GameMode gameMode = GameMode.CLASSIC;
            Score savedScore = mock(Score.class);

            when(scoreService.save(any(Score.class))).thenReturn(savedScore);
            when(leaderboardService.getTop3Leaderboard(gameMode)).thenReturn(List.of());

            // Act
            scoreRegistrationService.registerPlayerWin(player, numberOfTries, gameMode);

            // Assert
            verify(scoreService).save(scoreCaptor.capture());
            Score createdScore = scoreCaptor.getValue();
            assertEquals(player, createdScore.getPlayer());
            assertEquals(numberOfTries, createdScore.getNumberOfTries());
            assertEquals(gameMode, createdScore.getGameMode());

            verify(player).addTodayScore(gameMode, savedScore);
            verify(playerService).save(player);
            verify(cacheService).clear("paged-leaderboard");
            verify(cacheService).evictCacheEntry("top-3-leaderboard", gameMode.name());
        }
    }

    @Nested
    class RegisterPlayerWinWithTimestampTests {

        @Test
        void registerPlayerWinWithTimestamp_validData_savesScoreUpdatesPlayerAndClearsCaches() {
            // Arrange
            Player player = mock(Player.class);
            int numberOfTries = 4;
            GameMode gameMode = GameMode.QUOTES;
            Instant timestamp = Instant.now().minusSeconds(3600);
            Score savedScore = mock(Score.class);

            when(scoreService.save(any(Score.class))).thenReturn(savedScore);
            LeaderboardEntryDto top3Entry = mock(LeaderboardEntryDto.class);
            when(top3Entry.scoreTimeStamp()).thenReturn(timestamp.plusSeconds(100));
            when(leaderboardService.getTop3Leaderboard(gameMode)).thenReturn(List.of(top3Entry, top3Entry, top3Entry));

            // Act
            Score result = scoreRegistrationService.registerPlayerWinWithTimestamp(player, numberOfTries, gameMode, timestamp);

            // Assert
            assertEquals(savedScore, result);
            verify(scoreService).save(scoreCaptor.capture());
            assertEquals(timestamp, scoreCaptor.getValue().getScoreTimestamp());

            verify(player).addTodayScore(gameMode, savedScore);
            verify(playerService).save(player);
            verify(cacheService).clear("paged-leaderboard");
            verify(cacheService).evictCacheEntry("top-3-leaderboard", gameMode.name()); // USUNIĘTO NEVER()
        }
    }
}