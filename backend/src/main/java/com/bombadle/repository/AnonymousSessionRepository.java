package com.bombadle.repository;

import com.bombadle.entity.AnonymousSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AnonymousSessionRepository extends JpaRepository<AnonymousSession, UUID> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE anonymous_session CASCADE", nativeQuery = true)
    void truncateTable();

    @Modifying
    @Query("UPDATE AnonymousSession p SET p.lastActiveAt = :now WHERE p.id IN :ids")
    void updateLastActiveAtBulk(@Param("ids") Set<UUID> ids, @Param("now") Instant now);

    int countByLastActiveAtAfter(Instant threshold);

    /**
     * Counts anonymous sessions whose {@code completed_modes_today} JSONB array contains the given
     * mode. The set is cleared by the daily reset (the table is truncated), so this is inherently a
     * "solved today" count and needs no date filtering.
     */
    @Query(value = "SELECT COUNT(*) FROM anonymous_session WHERE jsonb_exists(completed_modes_today, :mode)",
            nativeQuery = true)
    long countByCompletedModeToday(@Param("mode") String mode);
}
