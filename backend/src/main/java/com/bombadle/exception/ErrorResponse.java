package com.bombadle.exception;

public record ErrorResponse(
        int statusCode,
        String error,
        String message
) {}