package com.bombadle.dto;

public record GuessResponse(boolean correct, GuessAttempt guessAttempt) {}
