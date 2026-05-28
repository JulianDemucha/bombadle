package com.bombadle.repository;

import com.bombadle.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findAllByActorIdOrderByCreatedAtDesc(Long actorId);
    Page<AdminAuditLog> findAllByActorId(Long actorId, Pageable pageable);
}
