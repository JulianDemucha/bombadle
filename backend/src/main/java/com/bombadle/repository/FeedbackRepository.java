package com.bombadle.repository;

import com.bombadle.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Modifying
    @Query("DELETE FROM Feedback f WHERE f.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE Feedback f SET f.playerId = null WHERE f.playerId = :playerId")
    void nullifyPlayerIdByPlayerId(@Param("playerId") Long playerId);
}
