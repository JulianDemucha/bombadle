package com.bombadle.repository;

import com.bombadle.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.Set;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByLogin(String Login);
    Optional<Player> findByEmail(String email);
    List<Player> findAllByOrderByIdAsc();
    Page<Player> findAllByOrderByIdAsc(Pageable pageable);
    Boolean existsByLogin(String Login);
    Boolean existsByEmail(String email);
    List<Player> findAllByMarkedForDeletionAtBefore(Instant cutoff);

    @Modifying
    @Query("DELETE FROM Player p WHERE p.id = :id")
    int deletePlayerById(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE Player p SET p.hasGuessedToday = false, p.todayScore = null")
    void resetAllScores();

    @Modifying
    @Query("UPDATE Player p SET p.lastActiveAt = :now WHERE p.id IN :ids")
    void updateLastActiveAtBulk(@Param("ids") Set<Long> ids, @Param("now") Instant now);

    int countByLastActiveAtAfter(Instant threshold);
}
