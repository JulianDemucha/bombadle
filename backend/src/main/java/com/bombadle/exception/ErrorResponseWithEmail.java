package com.bombadle.exception;

public record ErrorResponseWithEmail(int statusCode, String error, String message, String email) {
}
