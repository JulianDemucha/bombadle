package com.bombadle.service.admin;

import com.bombadle.dto.AdminAuditLogDto;
import com.bombadle.entity.AdminAuditLog;
import com.bombadle.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    public Optional<AdminAuditLogDto> getById(Long id) {
        return adminAuditLogRepository.findById(id)
                .map(AdminAuditLogDto::toDto);
    }

    public List<AdminAuditLogDto> getByActorId(Long actorId) {
        return adminAuditLogRepository.findAllByActorIdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(AdminAuditLogDto::toDto)
                .toList();
    }

    public Page<AdminAuditLogDto> getByActorId(Long actorId, Pageable pageable) {
        return adminAuditLogRepository.findAllByActorId(actorId, pageable)
                .map(AdminAuditLogDto::toDto);
    }


}
