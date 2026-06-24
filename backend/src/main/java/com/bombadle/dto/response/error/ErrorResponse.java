package com.bombadle.dto.response.error;

public record ErrorResponse(
        int statusCode,
        String error,
        String message
) {}