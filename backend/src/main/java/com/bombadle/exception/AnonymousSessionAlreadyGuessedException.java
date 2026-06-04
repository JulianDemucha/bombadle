package com.bombadle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AnonymousSessionAlreadyGuessedException extends RuntimeException {
    public AnonymousSessionAlreadyGuessedException() {
        super("This anonymous session has already been used to guess the card");
    }
}
