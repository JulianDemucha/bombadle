package com.bombadle.service.scheduling;

import com.bombadle.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationTokenCleanupScheduler {

    private final VerificationTokenRepository tokenRepository;

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired verification tokens...");
        try {
            tokenRepository.deleteAllExpiredSince(Instant.now());
            log.info("Successfully completed cleanup of expired tokens.");
        } catch (Exception e) {
            log.error("Error occurred during token cleanup: {}", e.getMessage());
        }
    }
}