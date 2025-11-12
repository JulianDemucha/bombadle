package com.bombadle.dto.mapper;
import com.bombadle.dto.PlayerDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PlayerMapper {

    public PlayerDto toDto(Player player) {
        return PlayerDto.builder()
                .id(player.getId())
                .login(player.getLogin())
                .email(player.getEmail())
                .role(player.getRole().toString())
                .avatarImage(player.getAvatarImage().toString())
                .createdAt(player.getCreatedAt().toString())
                .lastLoginAt(player.getLastLoginAt().toString())
                .hasGuessedToday(player.getHasGuessedToday())
                .todayScore(Optional.ofNullable(player.getTodayScore())
                        .map(Score::getScoreTimestamp)
                        .map(Object::toString)
                        .orElse(null))
                .totalGuesses(player.getTotalGuesses())
                .authProvider(player.getAuthProvider().toString())
                .build();
        /*
                ( for createdAt, lastLoginAt and todayScore )
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
    }

    public List<PlayerDto> toDto(List<Player> players) {
        List<PlayerDto> playerDtos = new ArrayList<>();
        for (Player player : players) {
            playerDtos.add(toDto(player));
        }
        return playerDtos;
    }


}
