package com.bombadle.exception;

public class ExpiredOtpException extends RuntimeException {
    public ExpiredOtpException(String message) {
        super(message);
    }
    public ExpiredOtpException() {
      super("Verification code has expired");
    }
}
