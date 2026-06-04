package com.bombadle.service.scheduling;

import com.bombadle.service.auth.RefreshTokenService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private RefreshTokenCleanupScheduler scheduler;

    @Nested
    class ScheduleCleanupTests {

        @Test
        void scheduleCleanup_executesSuccessfully_callsServiceWithCorrectCutoff() {
            // Arrange
            int expectedCutoff = 3600; // 60 * 60
            int mockRemovedCount = 5;
            when(refreshTokenService.deleteRevokedRefreshTokens(expectedCutoff)).thenReturn(mockRemovedCount);

            // Act
            scheduler.scheduleCleanup();

            // Assert
            verify(refreshTokenService).deleteRevokedRefreshTokens(expectedCutoff);
        }

        @Test
        void scheduleCleanup_serviceThrowsException_propagatesException() {
            // Arrange
            int expectedCutoff = 3600;
            when(refreshTokenService.deleteRevokedRefreshTokens(expectedCutoff))
                    .thenThrow(new RuntimeException("Database connection failure"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> scheduler.scheduleCleanup());
            verify(refreshTokenService).deleteRevokedRefreshTokens(expectedCutoff);
        }
    }
}