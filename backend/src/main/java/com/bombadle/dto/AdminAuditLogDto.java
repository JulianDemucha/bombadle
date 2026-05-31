package com.bombadle.dto;

import com.bombadle.entity.AdminAuditLog;

import java.time.Instant;

public record AdminAuditLogDto(
        Long id,
        Long actorId,
        String actionType,
        String description,
        Instant createdAt
) {

    public static AdminAuditLogDto toDto(AdminAuditLog log) {
        return new AdminAuditLogDto(
                log.getId(),
                log.getActorId(),
                log.getActionType(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }
}

