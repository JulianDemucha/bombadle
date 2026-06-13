package com.bombadle.dto.response;

import com.bombadle.dto.GuessAttempt;
import lombok.Builder;

@Builder
public record GuessResponse(boolean correct, GuessAttempt guessAttempt) {}
