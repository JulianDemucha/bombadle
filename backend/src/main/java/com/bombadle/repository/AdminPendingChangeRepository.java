package com.bombadle.repository;

import com.bombadle.entity.AdminPendingChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminPendingChangeRepository extends JpaRepository<AdminPendingChange, Long> {
    List<AdminPendingChange> findAllByOrderByCreatedAtAsc();

    Optional<AdminPendingChange> findFirstByActionKey(String actionKey);
}
