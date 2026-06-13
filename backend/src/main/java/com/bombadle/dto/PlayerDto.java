package com.bombadle.dto;

import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import lombok.Builder;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
public record PlayerDto (
        Long id,
        String displayName,
        String email,
        String role,
        String avatarImage,
        String createdAt,
        String lastLoginAt,
        Set<GameMode> completedModesToday,
        Map<GameMode, String> todayScoresTimestamps,
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
                .completedModesToday(player.getCompletedModesToday())
                .todayScoresTimestamps(
                        player.getTodayScores().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().getScoreTimestamp().toString()
                                ))
                )
                .totalGuesses(player.getTotalSuccessfulGuesses())
                .authProvider(player.getAuthProvider().toString())
                .hasPassword(player.getPasswordHash() != null && !player.getPasswordHash().isBlank())
                .build();
        /*
                ( for createdAt, lastLoginAt and todayScoresTimestamps )
                example toString output: 2025-07-24T15:30:45
                24=day T=separator 15=hour 30=minutes 45=seconds
                 */
    }
}