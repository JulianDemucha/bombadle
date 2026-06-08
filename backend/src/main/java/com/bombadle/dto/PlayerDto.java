package com.bombadle.dto;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import lombok.Builder;

import java.util.Optional;

@Builder
public record PlayerDto (
    Long id,
    String displayName,
    String email,
    String role,
    String avatarImage,
    String createdAt,
    String lastLoginAt,
    boolean hasGuessedToday,
    String todayScore,
    int totalGuesses,
    String authProvider,
    boolean hasPassword
){

    public static PlayerDto toDto(Player player) {
        return PlayerDto.builder()
                .id(player.getId())
                .displayName(player.getDisplayName())
                .email(player.getEmail())
                .role(player.getRole().toString())
                .avatarImage(player.getAvatarImage().toString())
                .createdAt(player.getCreatedAt().toString())
                .lastLoginAt(player.getLastActiveAt().toString())
                .hasGuessedToday(player.getHasGuessedToday())
                .todayScore(Optional.ofNullable(player.getTodayScore())
                        .map(Score::getScoreTimestamp)
                        .map(Object::toString)
                        .orElse(null))
                .totalGuesses(player.getTotalSuccessfulGuesses())
                .authProvider(player.getAuthProvider().toString())
                .hasPassword(player.getPasswordHash() != null && !player.getPasswordHash().isBlank())
                .build();
        /*
                ( for createdAt, lastLoginAt and todayScore )
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
    }
}
