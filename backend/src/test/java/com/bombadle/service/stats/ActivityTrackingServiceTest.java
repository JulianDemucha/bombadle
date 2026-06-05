package com.bombadle.service.stats;

import com.bombadle.entity.ActivitySnapshot;
import com.bombadle.repository.ActivitySnapshotRepository;
import com.bombadle.repository.AnonymousSessionRepository;
import com.bombadle.repository.PlayerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityTrackingServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private AnonymousSessionRepository anonymousSessionRepository;

    @Mock
    private ActivitySnapshotRepository snapshotRepository;

    @InjectMocks
    private ActivityTrackingService activityTrackingService;

    @Captor
    private ArgumentCaptor<Set<Long>> playerSetCaptor;

    @Captor
    private ArgumentCaptor<Set<UUID>> anonymousSetCaptor;

    @Captor
    private ArgumentCaptor<ActivitySnapshot> snapshotCaptor;

    @Nested
    class FlushActivityToDatabaseTests {

        @Test
        void flushActivityToDatabase_buffersAreEmpty_doesNotCallRepositories() {
            // Arrange
            // No activity marked

            // Act
            activityTrackingService.flushActivityToDatabase();

            // Assert
            verifyNoInteractions(playerRepository);
            verifyNoInteractions(anonymousSessionRepository);
        }

        @Test
        void flushActivityToDatabase_playersBufferHasItems_callsPlayerRepositoryOnly() {
            // Arrange
            Long playerId1 = 10L;
            Long playerId2 = 20L;
            activityTrackingService.markPlayerActive(playerId1);
            activityTrackingService.markPlayerActive(playerId2);

            // Act
            activityTrackingService.flushActivityToDatabase();

            // Assert
            verify(playerRepository).updateLastActiveAtBulk(playerSetCaptor.capture(), any(Instant.class));
            Set<Long> capturedPlayers = playerSetCaptor.getValue();

            assertEquals(2, capturedPlayers.size());
            assertTrue(capturedPlayers.contains(playerId1));
            assertTrue(capturedPlayers.contains(playerId2));

            verifyNoInteractions(anonymousSessionRepository);
        }

        @Test
        void flushActivityToDatabase_anonymousBufferHasItems_callsAnonymousRepositoryOnly() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            activityTrackingService.markAnonymousActive(sessionId);

            // Act
            activityTrackingService.flushActivityToDatabase();

            // Assert
            verify(anonymousSessionRepository).updateLastActiveAtBulk(anonymousSetCaptor.capture(), any(Instant.class));
            Set<UUID> capturedAnonymous = anonymousSetCaptor.getValue();

            assertEquals(1, capturedAnonymous.size());
            assertTrue(capturedAnonymous.contains(sessionId));

            verifyNoInteractions(playerRepository);
        }

        @Test
        void flushActivityToDatabase_bothBuffersHaveItems_callsBothRepositories() {
            // Arrange
            Long playerId = 5L;
            UUID sessionId = UUID.randomUUID();

            activityTrackingService.markPlayerActive(playerId);
            activityTrackingService.markAnonymousActive(sessionId);

            // Act
            activityTrackingService.flushActivityToDatabase();

            // Assert
            verify(playerRepository).updateLastActiveAtBulk(anySet(), any(Instant.class));
            verify(anonymousSessionRepository).updateLastActiveAtBulk(anySet(), any(Instant.class));
        }

        @Test
        void flushActivityToDatabase_calledTwice_secondCallDoesNotFlushOldData() {
            // Arrange
            Long playerId = 5L;
            activityTrackingService.markPlayerActive(playerId);

            // Act
            activityTrackingService.flushActivityToDatabase(); // First flush
            activityTrackingService.flushActivityToDatabase(); // Second flush

            // Assert
            // Repository should only be called once during the first flush
            verify(playerRepository).updateLastActiveAtBulk(anySet(), any(Instant.class));
        }
    }

    @Nested
    class CreateActivitySnapshotTests {

        @Test
        void createActivitySnapshot_fetchesCounts_savesSnapshotCorrectly() {
            // Arrange
            int expectedLoggedInCount = 15;
            int expectedAnonymousCount = 42;

            when(playerRepository.countByLastActiveAtAfter(any(Instant.class))).thenReturn(expectedLoggedInCount);
            when(anonymousSessionRepository.countByLastActiveAtAfter(any(Instant.class))).thenReturn(expectedAnonymousCount);

            // Act
            activityTrackingService.createActivitySnapshot();

            // Assert
            verify(snapshotRepository).save(snapshotCaptor.capture());
            ActivitySnapshot savedSnapshot = snapshotCaptor.getValue();

            assertNotNull(savedSnapshot);
            assertNotNull(savedSnapshot.getTimestamp());
            assertEquals(expectedLoggedInCount, savedSnapshot.getLoggedInActiveCount());
            assertEquals(expectedAnonymousCount, savedSnapshot.getAnonymousActiveCount());
        }
    }
}