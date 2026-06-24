package com.bombadle.service.admin;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.dto.request.AdminCacheFlushRequest;
// Pamiętaj o imporcie odpowiedniej klasy/interfejsu CacheService,
// który posiada metody evictAllCaches() oraz evictCache()
import com.bombadle.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCacheService {
    private final ApplicationConfigProperties.CacheConfig cacheConfig;
    private final AdminAuditService adminAuditService;

    private final CacheService cacheService;

    public void flushCache(long actorId, AdminCacheFlushRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        boolean flushAll = Boolean.TRUE.equals(request.flushAll());
        String cacheName = request.cacheName();

        if (!flushAll && (cacheName == null || cacheName.isBlank())) {
            throw new IllegalArgumentException("cacheName is required unless flushAll is true");
        }
        if (!flushAll && !cacheConfig.specs().containsKey(cacheName)) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        if (flushAll) {
            cacheService.evictAllCaches();
        } else {
            cacheService.evictCache(cacheName);
        }

        String actionType = flushAll ? "flush_cache_all" : "flush_cache_" + cacheName;

        adminAuditService.logAction(actorId, actionType, null);
    }
}