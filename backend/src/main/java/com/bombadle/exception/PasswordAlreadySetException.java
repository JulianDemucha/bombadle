package com.bombadle.exception;

public class PasswordAlreadySetException extends RuntimeException {
    public PasswordAlreadySetException() {
        super("Password already set");
    }
}
