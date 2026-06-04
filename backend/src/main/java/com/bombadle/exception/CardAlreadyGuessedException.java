package com.bombadle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CardAlreadyGuessedException extends RuntimeException {
    public CardAlreadyGuessedException() {
        super("Card already guessed today");
    }
}
