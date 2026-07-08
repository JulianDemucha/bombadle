package com.bombadle.dto;

/**
 * Compact statistics shown on the main card in player settings.
 */
public record BasicStatisticsDto(
        int currentSuperstreak,
        int totalGuesses,
        int totalTop3Finishes
) {
}
