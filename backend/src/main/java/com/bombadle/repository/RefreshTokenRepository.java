package com.bombadle.repository;

import com.bombadle.entity.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String token);

    @Modifying
    @Query("delete from RefreshToken t where t.revoked = true and t.revokedAt < :cutoff")
    int deleteRevokedBeforeCutoff(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from RefreshToken t where t.player.id = :playerId")
    int deleteByPlayerId(@Param("playerId") Long playerId);

    Optional<RefreshToken> findByPlayerId(Long playerId);
}
