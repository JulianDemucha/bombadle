package com.bombadle.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AnonymousGuessResponse (UUID anonymousSessionId, GuessResponse guessResponse) {}
