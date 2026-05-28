package com.bombadle.dto;

import lombok.Builder;

@Builder
public record GuessResponse(boolean correct, GuessAttempt guessAttempt) {}
