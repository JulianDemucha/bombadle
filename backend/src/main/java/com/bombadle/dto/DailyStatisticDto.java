package com.bombadle.dto;

import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;

import java.time.Instant;

/**
 * A single recorded solve, used to build the historical charts.
 *
 * @param puzzleDate ISO calendar day (yyyy-MM-dd) of the puzzle, on the 07:00 Europe/Warsaw boundary.
 */
public record DailyStatisticDto(
        GameMode gameMode,
        String puzzleDate,
        Instant solvedAt,
        int numberOfTries,
        int leaderboardPosition,
        int totalParticipants
) {

    public static DailyStatisticDto toDto(PlayerDailyStatistic statistic) {
        return new DailyStatisticDto(
                statistic.getGameMode(),
                statistic.getPuzzleDate().toString(),
                statistic.getSolvedAt(),
                statistic.getNumberOfTries(),
                statistic.getLeaderboardPosition(),
                statistic.getTotalParticipants()
        );
    }
}
