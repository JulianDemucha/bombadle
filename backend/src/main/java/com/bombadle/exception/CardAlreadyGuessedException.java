package com.bombadle.exception;

public class CardAlreadyGuessedException extends RuntimeException {
    public CardAlreadyGuessedException(String message) {
        super(message);
    }
    public CardAlreadyGuessedException() {
        super("You have already guessed today's card. Please wait until 7pm to play again.");
    }

}
