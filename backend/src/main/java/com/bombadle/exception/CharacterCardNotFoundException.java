package com.bombadle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CharacterCardNotFoundException extends RuntimeException {
    public CharacterCardNotFoundException(Long id) {
        super("Character card with id " + id + " not found");
    }
}
