package com.bombadle.repository;

import com.bombadle.entity.AnonymousSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface AnonymousSessionRepository extends JpaRepository<AnonymousSession, UUID> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE anonymous_session CASCADE", nativeQuery = true)
    void truncateTable();
}
