package com.bombadle.service.scheduling;

import com.bombadle.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationTokenCleanupSchedulerTest {

    @Mock
    private VerificationTokenRepository tokenRepository;

    @InjectMocks
    private VerificationTokenCleanupScheduler scheduler;

    @Nested
    class CleanupExpiredTokensTests {

        @Test
        void cleanupExpiredTokens_executesSuccessfully_callsRepository() {
            // Act
            scheduler.cleanupExpiredTokens();

            // Assert
            verify(tokenRepository).deleteAllExpiredSince(any(Instant.class));
        }

        @Test
        void cleanupExpiredTokens_repositoryThrowsException_catchesExceptionAndDoesNotThrow() {
            // Arrange
            doThrow(new RuntimeException("Database connection failed"))
                    .when(tokenRepository).deleteAllExpiredSince(any(Instant.class));

            // Act & Assert
            assertDoesNotThrow(() -> scheduler.cleanupExpiredTokens());

            verify(tokenRepository).deleteAllExpiredSince(any(Instant.class));
        }
    }
}