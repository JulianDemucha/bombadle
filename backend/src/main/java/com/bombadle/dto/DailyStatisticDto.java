package com.bombadle.dto;

import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;

import java.time.Instant;

/**
 * A single recorded solve, used to build the historical charts.
 *
 * @param puzzleDate ISO calendar day (yyyy-MM-dd) of the puzzle, on the 07:00 Europe/Warsaw boundary.
 * @param percentile {@code leaderboardPosition / totalSolvers} against the finalized end-of-day solver
 *                   count (lower is better); {@code null} for the current in-progress day, which has
 *                   no aggregate row yet.
 */
public record DailyStatisticDto(
        GameMode gameMode,
        String puzzleDate,
        Instant solvedAt,
        int numberOfTries,
        int leaderboardPosition,
        Double percentile
) {

    public static DailyStatisticDto toDto(PlayerDailyStatistic statistic, Double percentile) {
        return new DailyStatisticDto(
                statistic.getGameMode(),
                statistic.getPuzzleDate().toString(),
                statistic.getSolvedAt(),
                statistic.getNumberOfTries(),
                statistic.getLeaderboardPosition(),
                percentile
        );
    }
}
