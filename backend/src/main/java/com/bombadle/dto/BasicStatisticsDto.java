package com.bombadle.dto;

/**
 * Compact statistics shown on the main card in player settings.
 *
 * @param averageLeaderboardPercentile mean of {@code position / totalParticipants} across all
 *                                     recorded solves (lower is better); {@code null} when the
 *                                     player has no recorded history yet.
 */
public record BasicStatisticsDto(
        int currentSuperstreak,
        int totalGuesses,
        Double averageLeaderboardPercentile,
        int totalTop3Finishes
) {
}
