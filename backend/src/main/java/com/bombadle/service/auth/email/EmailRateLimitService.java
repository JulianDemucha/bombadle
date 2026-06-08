package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.exception.EmailRateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailRateLimitService {
    private final ApplicationConfigProperties.EmailConfig emailConfig;

    // Key: email address, Value: timestamp of the last sent email
    private final Map<String, Instant> emailLocks = new ConcurrentHashMap<>();

    public void enforceRateLimit(String email) {
        long cooldownSeconds = emailConfig.emailRateLimitSeconds();
        String normalizedEmail = email.toLowerCase();
        Instant lastSent = emailLocks.get(normalizedEmail);

        if (lastSent != null && Instant.now().isBefore(lastSent.plusSeconds(cooldownSeconds))) {
            long secondsLeft = lastSent.plusSeconds(cooldownSeconds).getEpochSecond() - Instant.now().getEpochSecond();
            throw new EmailRateLimitException("You must wait " + secondsLeft + " seconds before sending another email.");
        }

        emailLocks.put(normalizedEmail, Instant.now());
    }
}