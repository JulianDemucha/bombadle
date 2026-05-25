package com.bombadle.exception;

public class AdminOperationNotAllowedException extends RuntimeException {
    public AdminOperationNotAllowedException(String message) {
        super(message);
    }
}

