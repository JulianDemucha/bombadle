package com.bombadle.dto.request;

public record AdminUserUpdateRequest(
        String login,
        String avatarImage,
        Integer totalSuccessfulGuesses,
        Boolean clearTodayScore
) {
}

