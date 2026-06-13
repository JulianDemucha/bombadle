package com.bombadle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyGuessedException extends RuntimeException {
    public UserAlreadyGuessedException() {
        super("Card already guessed in that mode today");
    }
}
