package com.bombadle.repository;

import com.bombadle.entity.AdminPendingChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPendingChangeRepository extends JpaRepository<AdminPendingChange, Long> {
    List<AdminPendingChange> findAllByOrderByCreatedAtAsc();

    java.util.Optional<AdminPendingChange> findFirstByActionKey(String actionKey);
}
