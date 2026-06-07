package com.bombadle.service.scheduling;

import com.bombadle.service.auth.cookie.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenService refreshTokenService;
    final int secondsBeforeCutoff = 60 * 60;

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void scheduleCleanup() {
        int removed = refreshTokenService.deleteRevokedRefreshTokens(secondsBeforeCutoff);
        log.info("Removed {} refresh tokens revoked at least {} seconds ago", removed, secondsBeforeCutoff);
    }
}
