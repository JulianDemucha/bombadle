package com.bombadle.exception;

public class EmailRateLimitException extends RuntimeException {
    public EmailRateLimitException(String message) {
        super(message);
    }
}