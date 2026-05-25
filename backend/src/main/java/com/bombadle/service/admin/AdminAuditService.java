package com.bombadle.service.admin;

import com.bombadle.entity.AdminAuditLog;
import com.bombadle.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditLogRepository adminAuditLogRepository;

    public void logAction(Long actorId, String actionType, String description) {
        AdminAuditLog log = AdminAuditLog.builder()
                .actorId(actorId)
                .actionType(actionType)
                .description(description)
                .createdAt(Instant.now())
                .build();
        adminAuditLogRepository.save(log);
    }
}
