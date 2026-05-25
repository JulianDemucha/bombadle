package com.bombadle.dto;

import java.time.Instant;

public record AdminPendingCardChangeDto(
        String actionType,
        String changeType,
        Long cardId,
        String cardName,
        Instant createdAt
) {
}

