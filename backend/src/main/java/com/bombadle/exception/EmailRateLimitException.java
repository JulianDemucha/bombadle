package com.bombadle.exception;

public class EmailRateLimitException extends RuntimeException {

    private final long secondsToWait;

    public EmailRateLimitException(String message, long secondsToWait) {
        super(message);
        this.secondsToWait = secondsToWait;
    }

    public long getSecondsToWait() {
        return secondsToWait;
    }
}