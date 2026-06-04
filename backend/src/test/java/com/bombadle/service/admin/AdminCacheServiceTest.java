package com.bombadle.service.admin;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.dto.queue.PendingCacheFlushPayload;
import com.bombadle.dto.request.AdminCacheFlushRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminCacheServiceTest {

    @InjectMocks
    private AdminCacheService cacheService;

    @Mock
    private ApplicationConfigProperties.CacheConfig cacheConfig;

    @Mock
    private AdminAuditService auditService;

    @Mock
    private AdminChangeQueueService changeQueueService;

    @Nested
    class EnqueueFlushTests {

        @Test
        void enqueueFlush_requestNull_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    cacheService.enqueueFlush(1L, null));
        }

        @Test
        void enqueueFlush_cacheNameIsNull_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    cacheService.enqueueFlush(
                            1L,
                            new AdminCacheFlushRequest(
                                    null,
                                    false
                            )
                    )
            );
        }

        @Test
        void enqueueFlush_cacheNameIsBlank_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    cacheService.enqueueFlush(
                            1L,
                            new AdminCacheFlushRequest(
                                    "",
                                    false
                            )
                    )
            );
        }

        @Test
        void enqueueFlush_flushAllIsTrue_enqueuesAndLogsFlushAllProperly() {
            // Act
            cacheService.enqueueFlush(
                    1L,
                    new AdminCacheFlushRequest(
                            "sigma_cache",
                            true
                    )
            );

            // Assert
            ArgumentCaptor<PendingCacheFlushPayload> payloadCaptor = ArgumentCaptor.forClass(PendingCacheFlushPayload.class);

            verify(changeQueueService).enqueue(eq("flush_cache_all"), eq("cache:all"), payloadCaptor.capture());
            assertNotNull(payloadCaptor.getValue());
            assertEquals("sigma_cache", payloadCaptor.getValue().cacheName());
            assertTrue(payloadCaptor.getValue().flushAll());

            verify(auditService).logAction(eq(1L), eq("flush_cache_all"), isNull());
        }

        @Test
        void enqueueFlush_validCacheNameProvidedAndFlushAllIsFalse_enqueuesSpecificCacheFlush() {
            // Arrange
            when(cacheConfig.specs()).thenReturn(Map.of(
                    "sigma_cache",
                    new ApplicationConfigProperties.CacheSpec(
                            Duration.ofMinutes(30),
                            5L
                    ),
                    "not_sigma_cache",
                    new ApplicationConfigProperties.CacheSpec(
                            Duration.ofHours(3),
                            50L
                    )

            ));

            // Act
            cacheService.enqueueFlush(
                    1L,
                    new AdminCacheFlushRequest(
                            "sigma_cache",
                            false
                    )
            );

            // Assert
            ArgumentCaptor<PendingCacheFlushPayload> payloadCaptor = ArgumentCaptor.forClass(PendingCacheFlushPayload.class);

            verify(changeQueueService).enqueue(eq("flush_cache_sigma_cache"), eq("cache:sigma_cache"), payloadCaptor.capture());
            assertNotNull(payloadCaptor.getValue());
            assertEquals("sigma_cache", payloadCaptor.getValue().cacheName());
            assertFalse(payloadCaptor.getValue().flushAll());

            verify(auditService).logAction(1L, "flush_cache_sigma_cache", null);
        }

        @Test
        void enqueueFlush_invalidCacheNameProvidedAndFlushAllIsFalse_throwsIllegalArgumentException() {
            // Arrange
            when(cacheConfig.specs()).thenReturn(Map.of(
                    "not_sigma_cache",
                    new ApplicationConfigProperties.CacheSpec(
                            Duration.ofMinutes(30),
                            5L
                    ),
                    "also_not_sigma_cache",
                    new ApplicationConfigProperties.CacheSpec(
                            Duration.ofHours(5),
                            50L
                    )

            ));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                    cacheService.enqueueFlush(
                            1L,
                            new AdminCacheFlushRequest(
                                    "sigma_cache",
                                    false
                            )
                    )
            );
        }
    }
}