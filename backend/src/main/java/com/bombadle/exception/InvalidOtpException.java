package com.bombadle.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
    public InvalidOtpException() {
        super("Invalid verification code");
    }
}
