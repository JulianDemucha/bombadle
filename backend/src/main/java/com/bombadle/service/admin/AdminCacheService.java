package com.bombadle.service.admin;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.dto.request.AdminCacheFlushRequest;
import com.bombadle.dto.queue.PendingCacheFlushPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCacheService {
    private final ApplicationConfigProperties.CacheConfig cacheConfig;
    private final AdminAuditService adminAuditService;
    private final AdminChangeQueueService changeQueueService;

    public void enqueueFlush(long actorId, AdminCacheFlushRequest request) {
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

        PendingCacheFlushPayload payload = new PendingCacheFlushPayload(cacheName, flushAll);
        String actionType = flushAll ? "flush_cache_all" : "flush_cache_" + cacheName;
        String actionKey = flushAll ? "cache:all" : "cache:" + cacheName;
        changeQueueService.enqueue(actionType, actionKey, payload);
        adminAuditService.logAction(actorId, actionType, null);
    }
}
