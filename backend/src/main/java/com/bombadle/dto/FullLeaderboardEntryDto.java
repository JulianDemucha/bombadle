package com.bombadle.dto;

import com.bombadle.enums.AvatarImage;
import lombok.Builder;

import java.time.Instant;

/**
 * Entry for the full (paged) leaderboard. Unlike {@link LeaderboardEntryDto} (used by the Top3 and
 * current-user-rank views) it additionally carries {@code currentSuperstreak}, rendered as the
 * "superseria" column.
 */
@Builder
public record FullLeaderboardEntryDto(
        Long playerId,
        Long rank,
        String playerDisplayName,
        AvatarImage playerAvatarImage,
        Instant scoreTimeStamp,
        int numberOfTries,
        int currentStreak,
        int currentSuperstreak
) {
}
