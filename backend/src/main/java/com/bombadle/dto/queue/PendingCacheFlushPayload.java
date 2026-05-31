package com.bombadle.dto.queue;

public record PendingCacheFlushPayload(
        String cacheName,
        Boolean flushAll
) {
}

