package com.bombadle.dto;

import java.time.Instant;

public record AdminAuditLogDto(
        Long id,
        Long actorId,
        String actionType,
        String description,
        Instant createdAt
) {
}

