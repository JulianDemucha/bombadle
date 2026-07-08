package com.bombadle.repository;

import com.bombadle.entity.AccountRecoveryToken;
import com.bombadle.enums.EmailVerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRecoveryTokenRepository extends JpaRepository<AccountRecoveryToken, Long> {
    Optional<AccountRecoveryToken> findByDeletedAccountIdAndEmailVerificationType(Long deletedAccountId, EmailVerificationType emailVerificationType);
}
