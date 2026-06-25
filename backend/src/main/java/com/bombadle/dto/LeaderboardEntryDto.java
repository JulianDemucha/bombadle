package com.bombadle.dto;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;


@Builder
public record LeaderboardEntryDto(
        Long playerId,
        Long rank,
        String playerDisplayName,
        AvatarImage playerAvatarImage,
        Instant scoreTimeStamp,
        int numberOfTries,
        int currentStreak
) {


    public static LeaderboardEntryDto toDto(Score score) {
        return LeaderboardEntryDto.builder()
                .playerId(score.getPlayer().getId())
                .playerDisplayName(score.getPlayer().getDisplayName())
                .playerAvatarImage(score.getPlayer().getAvatarImage())
                .scoreTimeStamp(score.getScoreTimestamp())
                .numberOfTries(score.getNumberOfTries())
                .currentStreak(score.getPlayer().getCurrentStreak())
                .build();
    }

    public static LeaderboardEntryDto toDto(Player player, GameMode gameMode) {
        Score score = player.getTodayScore(gameMode)
                .orElseThrow(() -> new IllegalArgumentException("Player does not have a score for mode: " + gameMode));

        return LeaderboardEntryDto.builder()
                .playerId(player.getId())
                .playerDisplayName(player.getDisplayName())
                .playerAvatarImage(player.getAvatarImage())
                .scoreTimeStamp(score.getScoreTimestamp())
                .numberOfTries(score.getNumberOfTries())
                .currentStreak(player.getCurrentStreak())
                .build();
    }
}

