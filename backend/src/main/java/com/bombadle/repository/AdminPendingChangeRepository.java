package com.bombadle.repository;

import com.bombadle.entity.AdminPendingChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminPendingChangeRepository extends JpaRepository<AdminPendingChange, Long> {
    List<AdminPendingChange> findAllByOrderByCreatedAtAsc();

    Optional<AdminPendingChange> findFirstByActionKey(String actionKey);
}
