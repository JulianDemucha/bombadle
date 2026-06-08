package com.bombadle.repository;

import com.bombadle.entity.VerificationToken;
import com.bombadle.enums.EmailVerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByPlayerIdAndEmailVerificationType(Long player_id, EmailVerificationType emailVerificationType);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now")
    void deleteAllExpiredSince(Instant now);

}
