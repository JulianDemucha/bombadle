package com.bombadle.service.auth.email;

import com.bombadle.entity.Player;
import com.bombadle.entity.VerificationToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.exception.ExpiredOtpException;
import com.bombadle.exception.InvalidOtpException;
import com.bombadle.exception.OtpNotFoundException;
import com.bombadle.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {
    private final VerificationTokenRepository repo;

    public Optional<VerificationToken> findByPlayerIdAndEmailVerificationType(Long playerId, EmailVerificationType emailVerificationType) {
        return repo.findByPlayerIdAndEmailVerificationType(playerId, emailVerificationType);
    }

    public VerificationToken generateNewToken(Player player, EmailVerificationType verificationType, int tokenExpireMinutes) {
        deletePotentialOldToken(player.getId(), verificationType);

        VerificationToken token = VerificationToken.builder()
                .player(player)
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

    private void deletePotentialOldToken(Long playerId, EmailVerificationType verificationType) {
        Optional<VerificationToken> potentialOldToken = repo.findByPlayerIdAndEmailVerificationType(playerId, verificationType);;
        potentialOldToken.ifPresent(repo::delete);
    }

    @Transactional
    public void verifyAndConsume(Long playerId, EmailVerificationType type, String code) {
        VerificationToken token = repo.findByPlayerIdAndEmailVerificationType(playerId, type)
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
