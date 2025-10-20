package com.bombadle.dto;

import lombok.Builder;

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
    String totalGuesses,
    String authProvider
){}
