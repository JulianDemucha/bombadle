package com.bombadle.exception;

public class StageLockedException extends RuntimeException {
    public StageLockedException(String message) {
        super(message);
    }
}
