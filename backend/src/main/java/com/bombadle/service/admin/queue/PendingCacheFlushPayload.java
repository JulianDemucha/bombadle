package com.bombadle.service.admin.queue;

public record PendingCacheFlushPayload(
        String cacheName,
        Boolean flushAll
) {
}

