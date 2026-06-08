package com.bombadle.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnverifiedEmailException extends RuntimeException {
    private final String email;

    public UnverifiedEmailException(String message, String email) {
        super(message);
        this.email = email;
    }
}
