package com.bombadle.dto;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import lombok.Builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Builder
public record LeaderboardEntryDto(
        Long playerId,
        Long rank,
        String playerLogin,
        AvatarImage playerAvatarImage,
        Instant scoreTimeStamp,
        int numberOfTries,
        int wins
) {


    public static LeaderboardEntryDto toDto(Score score) {
        return LeaderboardEntryDto.builder()
                .playerId(score.getPlayer().getId())
                .playerAvatarImage(AvatarImage.valueOf(score.getPlayer().getAvatarImage().toString() + ".png"))
                .scoreTimeStamp(Instant.parse(score.getScoreTimestamp().toString()))
                /*
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
                .numberOfTries(score.getNumberOfTries())
                .build();
    }

    public static LeaderboardEntryDto toDto(Player player) {
        return LeaderboardEntryDto.builder()
                .playerId(player.getId())
                .playerAvatarImage(AvatarImage.valueOf(player.getAvatarImage().toString() + ".png"))
                .scoreTimeStamp(Instant.parse(player.getTodayScore().getScoreTimestamp().toString()))
                /*
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
                .numberOfTries(player.getTodayScore().getNumberOfTries())
                .build();
    }
}

