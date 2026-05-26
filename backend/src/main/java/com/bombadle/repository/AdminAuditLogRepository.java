package com.bombadle.repository;

import com.bombadle.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    java.util.List<AdminAuditLog> findAllByActorIdOrderByCreatedAtDesc(Long actorId);
    org.springframework.data.domain.Page<AdminAuditLog> findAllByActorId(Long actorId, org.springframework.data.domain.Pageable pageable);
}
