package com.bombadle.service.auth.email;

import com.bombadle.entity.AccountRecoveryToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.exception.ExpiredOtpException;
import com.bombadle.exception.InvalidOtpException;
import com.bombadle.exception.OtpNotFoundException;
import com.bombadle.repository.AccountRecoveryTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountRecoveryTokenService {
    private final AccountRecoveryTokenRepository repo;

    public AccountRecoveryToken generateNewToken(Long deletedAccountId, EmailVerificationType verificationType, int tokenExpireMinutes) {
        deletePotentialOldToken(deletedAccountId, verificationType);

        AccountRecoveryToken token = AccountRecoveryToken.builder()
                .deletedAccountId(deletedAccountId)
                .verificationCode(generateRandomVerificationCode())
                .emailVerificationType(verificationType)
                .expiresAt(Instant.now().plusSeconds(tokenExpireMinutes * 60L))
                .build();
        return repo.save(token);
    }

    private String generateRandomVerificationCode(){
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void deletePotentialOldToken(Long deletedAccountId, EmailVerificationType verificationType) {
        Optional<AccountRecoveryToken> potentialOldToken = repo.findByDeletedAccountIdAndEmailVerificationType(deletedAccountId, verificationType);
        potentialOldToken.ifPresent(repo::delete);
    }

    @Transactional
    public void verifyAndConsume(Long deletedAccountId, EmailVerificationType type, String code) {
        AccountRecoveryToken token = repo.findByDeletedAccountIdAndEmailVerificationType(deletedAccountId, type)
                .orElseThrow(() -> new OtpNotFoundException("Verification code not found or already used"));

        if (!token.getVerificationCode().equals(code)) {
            throw new InvalidOtpException("Invalid verification code");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            repo.delete(token);
            throw new ExpiredOtpException("Verification code has expired");
        }

        repo.delete(token);
    }
}
