package com.bombadle.dto;

import com.bombadle.enums.GameMode;

import java.util.Map;

/**
 * Full statistics for the dedicated statistics page.
 *
 * @param totalGuesses  all-time successful-guess counter from the player (includes QUOTES_STAGE_1).
 * @param guessesByMode per-mode solve counts derived from recorded daily statistics
 *                      (only CLASSIC, QUOTES_STAGE_2 and IMAGES; counted since stats tracking began).
 */
public record DetailedStatisticsDto(
        int totalGuesses,
        Map<GameMode, Integer> guessesByMode,
        int currentStreak,
        int longestStreak,
        int currentSuperstreak,
        int longestSuperstreak
) {
}
