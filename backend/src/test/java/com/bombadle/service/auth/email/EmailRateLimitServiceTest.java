package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.exception.EmailRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailRateLimitServiceTest {

    @Mock
    private ApplicationConfigProperties.EmailConfig emailConfig;

    @InjectMocks
    private EmailRateLimitService emailRateLimitService;

    @BeforeEach
    void setUp() {
        when(emailConfig.emailRateLimitSeconds()).thenReturn(60);
    }

    @Test
    void enforceRateLimit_firstTimeEmail_allowsSending() {
        assertDoesNotThrow(() -> emailRateLimitService.enforceRateLimit("new@mail.com"));
    }

    @Test
    void enforceRateLimit_emailSentTwiceWithinCooldown_throwsException() {
        emailRateLimitService.enforceRateLimit("test@mail.com");

        assertThrows(EmailRateLimitException.class, () -> emailRateLimitService.enforceRateLimit("test@mail.com"));
    }

    @Test
    void enforceRateLimit_emailSentAfterCooldown_allowsSending() {
        Map<String, Instant> mockedLocks = new ConcurrentHashMap<>();
        mockedLocks.put("test@mail.com", Instant.now().minusSeconds(61));
        ReflectionTestUtils.setField(emailRateLimitService, "emailLocks", mockedLocks);

        assertDoesNotThrow(() -> emailRateLimitService.enforceRateLimit("test@mail.com"));
    }

    @Test
    void enforceRateLimit_normalizesEmail() {
        emailRateLimitService.enforceRateLimit("TEST@mail.com");

        assertThrows(EmailRateLimitException.class, () -> emailRateLimitService.enforceRateLimit("test@mail.com"));
    }
}