package com.bombadle.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RateLimitErrorResponse(
        int statusCode,
        String error,
        String message,
        @JsonProperty("seconds-to-wait") long secondsToWait
) {}