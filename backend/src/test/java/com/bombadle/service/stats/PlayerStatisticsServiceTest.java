package com.bombadle.service.stats;

import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerStatisticsServiceTest {

    @Mock
    private PlayerDailyStatisticRepository playerDailyStatisticRepository;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private PlayerStatisticsService playerStatisticsService;

    @Captor
    private ArgumentCaptor<PlayerDailyStatistic> statisticCaptor;

    private static Player playerWithId(long id) {
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(id);
        return player;
    }

    private static Score score(GameMode gameMode, Instant timestamp, int numberOfTries) {
        return Score.builder()
                .scoreTimestamp(timestamp)
                .numberOfTries(numberOfTries)
                .gameMode(gameMode)
                .build();
    }

    @Nested
    class RecordDailyStatisticTests {

        @Test
        void recordDailyStatistic_validData_persistsSnapshotWithSolveTimePosition() {
            // Arrange
            Player player = playerWithId(1L);
            Instant solvedAt = Instant.parse("2026-06-24T12:00:00Z"); // 14:00 Europe/Warsaw -> puzzle day 2026-06-24
            Score score = score(GameMode.CLASSIC, solvedAt, 3);

            when(playerDailyStatisticRepository.existsByPlayerIdAndGameModeAndPuzzleDate(1L, GameMode.CLASSIC, LocalDate.of(2026, 6, 24)))
                    .thenReturn(false);
            when(leaderboardService.getPlayerRankById(GameMode.CLASSIC, 1L)).thenReturn(5L);
            when(leaderboardService.countParticipants(GameMode.CLASSIC)).thenReturn(20);

            // Act
            playerStatisticsService.recordDailyStatistic(player, score);

            // Assert
            verify(playerDailyStatisticRepository).save(statisticCaptor.capture());
            PlayerDailyStatistic saved = statisticCaptor.getValue();
            assertEquals(player, saved.getPlayer());
            assertEquals(GameMode.CLASSIC, saved.getGameMode());
            assertEquals(LocalDate.of(2026, 6, 24), saved.getPuzzleDate());
            assertEquals(solvedAt, saved.getSolvedAt());
            assertEquals(3, saved.getNumberOfTries());
            assertEquals(5, saved.getLeaderboardPosition());
            assertEquals(20, saved.getTotalParticipants());
        }

        @Test
        void recordDailyStatistic_quotesStageOne_isSkipped() {
            // Arrange
            Player player = mock(Player.class);
            Score score = score(GameMode.QUOTES_STAGE_1, Instant.parse("2026-06-24T12:00:00Z"), 2);

            // Act
            playerStatisticsService.recordDailyStatistic(player, score);

            // Assert
            verify(playerDailyStatisticRepository, never()).save(any());
            verifyNoInteractions(leaderboardService);
        }

        @Test
        void recordDailyStatistic_alreadyRecordedForThatDay_isSkipped() {
            // Arrange
            Player player = playerWithId(1L);
            Score score = score(GameMode.IMAGES, Instant.parse("2026-06-24T12:00:00Z"), 4);

            when(playerDailyStatisticRepository.existsByPlayerIdAndGameModeAndPuzzleDate(1L, GameMode.IMAGES, LocalDate.of(2026, 6, 24)))
                    .thenReturn(true);

            // Act
            playerStatisticsService.recordDailyStatistic(player, score);

            // Assert
            verify(playerDailyStatisticRepository, never()).save(any());
            verifyNoInteractions(leaderboardService);
        }
    }

    @Nested
    class ResolvePuzzleDateTests {

        @Test
        void resolvePuzzleDate_afternoonSolve_mapsToSameCalendarDay() {
            Instant solvedAt = Instant.parse("2026-06-24T12:00:00Z"); // 14:00 Europe/Warsaw
            assertEquals(LocalDate.of(2026, 6, 24), playerStatisticsService.resolvePuzzleDate(solvedAt));
        }

        @Test
        void resolvePuzzleDate_beforeResetBoundary_mapsToPreviousPuzzleDay() {
            Instant solvedAt = Instant.parse("2026-06-24T03:00:00Z"); // 05:00 Europe/Warsaw, before the 07:00 reset
            assertEquals(LocalDate.of(2026, 6, 23), playerStatisticsService.resolvePuzzleDate(solvedAt));
        }
    }
}
