package com.bombadle.service.stats;

import com.bombadle.dto.BasicStatisticsDto;
import com.bombadle.dto.DailyStatisticDto;
import com.bombadle.dto.DetailedStatisticsDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import com.bombadle.repository.PlayerRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerStatisticsServiceTest {

    @Mock
    private PlayerDailyStatisticRepository playerDailyStatisticRepository;

    @Mock
    private PlayerRepository playerRepository;

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
    class EvaluateDailyStreaksTests {

        private static Player playerWithStreaks(Set<GameMode> completedModesToday,
                                                int currentStreak, int longestStreak,
                                                int currentSuperstreak, int longestSuperstreak) {
            return Player.builder()
                    .completedModesToday(completedModesToday)
                    .currentStreak(currentStreak)
                    .longestStreak(longestStreak)
                    .currentSuperstreak(currentSuperstreak)
                    .longestSuperstreak(longestSuperstreak)
                    .build();
        }

        @Test
        void evaluateDailyStreaks_allModesCompleted_incrementsStreakAndSuperstreak() {
            Player player = playerWithStreaks(
                    Set.of(GameMode.CLASSIC, GameMode.IMAGES, GameMode.QUOTES_STAGE_1, GameMode.QUOTES_STAGE_2),
                    4, 4, 2, 5);
            when(playerRepository.findAll()).thenReturn(List.of(player));

            playerStatisticsService.evaluateDailyStreaks();

            assertEquals(5, player.getCurrentStreak());
            assertEquals(5, player.getLongestStreak());
            assertEquals(3, player.getCurrentSuperstreak());
            assertEquals(5, player.getLongestSuperstreak()); // unchanged: 3 < 5
            verify(playerRepository).saveAll(List.of(player));
        }

        @Test
        void evaluateDailyStreaks_someButNotAllModes_incrementsStreakAndResetsSuperstreak() {
            Player player = playerWithStreaks(Set.of(GameMode.CLASSIC), 4, 6, 3, 7);
            when(playerRepository.findAll()).thenReturn(List.of(player));

            playerStatisticsService.evaluateDailyStreaks();

            assertEquals(5, player.getCurrentStreak());
            assertEquals(6, player.getLongestStreak()); // unchanged: 5 < 6
            assertEquals(0, player.getCurrentSuperstreak()); // reset independently
            assertEquals(7, player.getLongestSuperstreak()); // preserved
        }

        @Test
        void evaluateDailyStreaks_noModesCompleted_resetsBothCurrentCountersPreservingMaxima() {
            Player player = playerWithStreaks(Set.of(), 9, 9, 4, 8);
            when(playerRepository.findAll()).thenReturn(List.of(player));

            playerStatisticsService.evaluateDailyStreaks();

            assertEquals(0, player.getCurrentStreak());
            assertEquals(9, player.getLongestStreak());
            assertEquals(0, player.getCurrentSuperstreak());
            assertEquals(8, player.getLongestSuperstreak());
        }
    }

    @Nested
    class ReadStatisticsTests {

        @Test
        void getBasicStatistics_withHistory_returnsLivePlayerFieldsAndAggregates() {
            Player player = Player.builder()
                    .currentSuperstreak(3)
                    .totalSuccessfulGuesses(42)
                    .build();
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            when(playerDailyStatisticRepository.findAveragePercentileByPlayerId(1L)).thenReturn(0.25);
            when(playerDailyStatisticRepository.countTop3FinishesByPlayerId(1L)).thenReturn(7L);

            BasicStatisticsDto result = playerStatisticsService.getBasicStatistics(1L);

            assertEquals(3, result.currentSuperstreak());
            assertEquals(42, result.totalGuesses());
            assertEquals(0.25, result.averageLeaderboardPercentile());
            assertEquals(7, result.totalTop3Finishes());
        }

        @Test
        void getBasicStatistics_noHistory_returnsNullPercentileAndZeroTop3() {
            Player player = Player.builder().currentSuperstreak(0).totalSuccessfulGuesses(0).build();
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            when(playerDailyStatisticRepository.findAveragePercentileByPlayerId(1L)).thenReturn(null);
            when(playerDailyStatisticRepository.countTop3FinishesByPlayerId(1L)).thenReturn(0L);

            BasicStatisticsDto result = playerStatisticsService.getBasicStatistics(1L);

            assertNull(result.averageLeaderboardPercentile());
            assertEquals(0, result.totalTop3Finishes());
        }

        @Test
        void getDetailedStatistics_returnsStreaksAndPerModeBreakdown() {
            Player player = Player.builder()
                    .totalSuccessfulGuesses(20)
                    .currentStreak(5)
                    .longestStreak(8)
                    .currentSuperstreak(2)
                    .longestSuperstreak(4)
                    .build();
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
            when(playerDailyStatisticRepository.countByPlayerIdGroupedByGameMode(1L)).thenReturn(List.of(
                    new Object[]{GameMode.CLASSIC, 5L},
                    new Object[]{GameMode.IMAGES, 3L}
            ));

            DetailedStatisticsDto result = playerStatisticsService.getDetailedStatistics(1L);

            assertEquals(20, result.totalGuesses());
            assertEquals(5, result.currentStreak());
            assertEquals(8, result.longestStreak());
            assertEquals(2, result.currentSuperstreak());
            assertEquals(4, result.longestSuperstreak());
            assertEquals(2, result.guessesByMode().size());
            assertEquals(5, result.guessesByMode().get(GameMode.CLASSIC));
            assertEquals(3, result.guessesByMode().get(GameMode.IMAGES));
        }

        @Test
        void getChartStatistics_mapsEntitiesToDtosInOrder() {
            PlayerDailyStatistic stat = PlayerDailyStatistic.builder()
                    .gameMode(GameMode.CLASSIC)
                    .puzzleDate(LocalDate.of(2026, 6, 24))
                    .solvedAt(Instant.parse("2026-06-24T12:00:00Z"))
                    .numberOfTries(3)
                    .leaderboardPosition(5)
                    .totalParticipants(20)
                    .build();
            when(playerDailyStatisticRepository.findByPlayerIdOrderByPuzzleDateAscGameModeAsc(1L))
                    .thenReturn(List.of(stat));

            List<DailyStatisticDto> result = playerStatisticsService.getChartStatistics(1L);

            assertEquals(1, result.size());
            DailyStatisticDto dto = result.get(0);
            assertEquals(GameMode.CLASSIC, dto.gameMode());
            assertEquals("2026-06-24", dto.puzzleDate());
            assertEquals(Instant.parse("2026-06-24T12:00:00Z"), dto.solvedAt());
            assertEquals(3, dto.numberOfTries());
            assertEquals(5, dto.leaderboardPosition());
            assertEquals(20, dto.totalParticipants());
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

    @Nested
    class DeleteAllByPlayerIdTests {

        @Test
        void deleteAllByPlayerId_delegatesToRepository() {
            playerStatisticsService.deleteAllByPlayerId(7L);

            verify(playerDailyStatisticRepository).deleteByPlayerId(7L);
        }
    }
}
