package com.bombadle.dto.request;

public record AdminCacheFlushRequest(
        String cacheName,
        Boolean flushAll
) {
}

