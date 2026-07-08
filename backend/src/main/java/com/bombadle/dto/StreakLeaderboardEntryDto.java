package com.bombadle.dto;

import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import lombok.Builder;

/**
 * Entry for the player-level streak rankings (by current streak and by current superstreak).
 * Unlike {@link LeaderboardEntryDto} / {@link FullLeaderboardEntryDto} it carries no
 * {@code scoreTimeStamp}/{@code numberOfTries}, since those are per-mode-score concepts that do not
 * apply to a streak ranking. Both streak values are included so a single DTO serves both rankings:
 * the streak board renders {@code currentStreak}, the superstreak board renders
 * {@code currentSuperstreak}.
 */
@Builder
public record StreakLeaderboardEntryDto(
        Long playerId,
        Long rank,
        String playerDisplayName,
        AvatarImage playerAvatarImage,
        int currentStreak,
        int currentSuperstreak
) {

    /**
     * Builds an entry from a player and a caller-supplied rank (rank is assigned by position in the
     * ordered result, not derived from the entity).
     */
    public static StreakLeaderboardEntryDto of(Player player, long rank) {
        return StreakLeaderboardEntryDto.builder()
                .playerId(player.getId())
                .rank(rank)
                .playerDisplayName(player.getDisplayName())
                .playerAvatarImage(player.getAvatarImage())
                .currentStreak(player.getCurrentStreak())
                .currentSuperstreak(player.getCurrentSuperstreak())
                .build();
    }
}
