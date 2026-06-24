package com.bombadle.service.admin;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.dto.request.AdminCacheFlushRequest;
import com.bombadle.service.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCacheServiceTest {

    @Mock
    private ApplicationConfigProperties.CacheConfig cacheConfig;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AdminCacheService adminCacheService;

    @Test
    void flushCache_whenRequestIsNull_throwsIllegalArgumentException() {
        // Arrange
        long actorId = 1L;
        AdminCacheFlushRequest request = null;

        // Act & Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adminCacheService.flushCache(actorId, request))
                .withMessage("Request is required");

        verifyNoInteractions(cacheService, adminAuditService);
    }

    @Test
    void flushCache_whenFlushAllFalseAndCacheNameIsBlank_throwsIllegalArgumentException() {
        // Arrange
        long actorId = 1L;
        // Poprawka: najpierw String cacheName, potem Boolean flushAll
        AdminCacheFlushRequest request = new AdminCacheFlushRequest("   ", false);

        // Act & Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adminCacheService.flushCache(actorId, request))
                .withMessage("cacheName is required unless flushAll is true");

        verifyNoInteractions(cacheService, adminAuditService);
    }

    @Test
    void flushCache_whenCacheNameIsUnknown_throwsIllegalArgumentException() {
        // Arrange
        long actorId = 1L;
        String invalidCacheName = "unknownCache";
        // Poprawka: najpierw String cacheName, potem Boolean flushAll
        AdminCacheFlushRequest request = new AdminCacheFlushRequest(invalidCacheName, false);

        when(cacheConfig.specs()).thenReturn(Map.of());

        // Act & Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adminCacheService.flushCache(actorId, request))
                .withMessage("Unknown cache name: " + invalidCacheName);

        verifyNoInteractions(cacheService, adminAuditService);
    }

    @Test
    void flushCache_whenFlushAllIsTrue_evictsAllCachesAndLogsAction() {
        // Arrange
        long actorId = 123L;
        // Poprawka: najpierw String cacheName, potem Boolean flushAll
        AdminCacheFlushRequest request = new AdminCacheFlushRequest(null, true);

        // Act
        adminCacheService.flushCache(actorId, request);

        // Assert
        verify(cacheService).evictAllCaches();
        verify(adminAuditService).logAction(actorId, "flush_cache_all", null);
        verifyNoMoreInteractions(cacheService);
    }

    @Test
    void flushCache_whenFlushAllIsFalseAndCacheNameIsValid_evictsSpecificCacheAndLogsAction() {
        // Arrange
        long actorId = 456L;
        String validCacheName = "playersCache";
        // Poprawka: użycie wartości 'false' zamiast słowa kluczowego 'boolean'
        AdminCacheFlushRequest request = new AdminCacheFlushRequest(validCacheName, false);

        when(cacheConfig.specs()).thenReturn(Map.of(
                validCacheName,
                new ApplicationConfigProperties.CacheSpec(
                        Duration.ofMinutes(1),
                        1L
                )
        ));

        // Act
        adminCacheService.flushCache(actorId, request);

        // Assert
        verify(cacheService).evictCache(validCacheName);
        verify(adminAuditService).logAction(actorId, "flush_cache_playersCache", null);
        verifyNoMoreInteractions(cacheService);
    }
}