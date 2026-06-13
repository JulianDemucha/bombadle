package com.bombadle.dto.response.error;

public record ErrorResponseWithEmail(int statusCode, String error, String message, String email) {
}
