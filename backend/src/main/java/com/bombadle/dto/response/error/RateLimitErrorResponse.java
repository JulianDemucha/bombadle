package com.bombadle.dto.response.error;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RateLimitErrorResponse(
        int statusCode,
        String error,
        String message,
        @JsonProperty("seconds-to-wait") long secondsToWait
) {}