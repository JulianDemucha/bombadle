package com.bombadle.repository;

import com.bombadle.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findAllByActorIdOrderByCreatedAtDesc(Long actorId);
    Page<AdminAuditLog> findAllByActorId(Long actorId, Pageable pageable);
}
