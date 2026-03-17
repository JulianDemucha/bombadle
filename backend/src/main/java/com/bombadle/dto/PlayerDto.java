package com.bombadle.dto;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import lombok.Builder;

import java.util.Optional;

@Builder
public record PlayerDto (
    Long id,
    String login,
    String email,
    String role,
    String avatarImage,
    String createdAt,
    String lastLoginAt,
    boolean hasGuessedToday,
    String todayScore,
    int totalGuesses,
    String authProvider
){

    public static PlayerDto toDto(Player player) {
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
                .totalGuesses(player.getTotalSuccessfulGuesses())
                .authProvider(player.getAuthProvider().toString())
                .build();
        /*
                ( for createdAt, lastLoginAt and todayScore )
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
    }
}
