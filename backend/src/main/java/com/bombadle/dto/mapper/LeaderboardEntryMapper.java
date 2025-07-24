package com.bombadle.dto.mapper;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LeaderboardEntryMapper {

    /* single DTOs */

    public LeaderboardEntryDto toDto(Score score) {
        return LeaderboardEntryDto.builder()
                .playerId(score.getPlayer().getId())
                .playerAvatarImage(score.getPlayer().getAvatarImage().toString() + ".png")
                .scoreTimeStamp(score.getScoreTimestamp().toString())
                /*
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
                .numberOfTries(score.getNumberOfTries())
                .build();
    }

    public LeaderboardEntryDto toDto(Player player) {
        return LeaderboardEntryDto.builder()
                .playerId(player.getId())
                .playerAvatarImage(player.getAvatarImage().toString() + ".png")
                .scoreTimeStamp(player.getTodayScore().getScoreTimestamp().toString())
                /*
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
                .numberOfTries(player.getTodayScore().getNumberOfTries())
                .build();
    }

    /* lists of DTOs */

    public List<LeaderboardEntryDto> PlayersToDto(List<Player> leaderboardByPlayers) {
        List<LeaderboardEntryDto> dtoLeaderboard = new ArrayList<LeaderboardEntryDto>();
        for (Player player : leaderboardByPlayers) {
            dtoLeaderboard.add(toDto(player));
        }
        return dtoLeaderboard;
    }

    public List<LeaderboardEntryDto> ScoresToDto(List<Score> leaderboardByScore) {
        List<LeaderboardEntryDto> dtoLeaderboard = new ArrayList<LeaderboardEntryDto>();
        for (Score score : leaderboardByScore) {
            dtoLeaderboard.add(toDto(score));
        }
        return dtoLeaderboard;
    }
}
