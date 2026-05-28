package com.bombadle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
public record AnonymousGuessResponse (UUID anonymousSessionId, GuessResponse guessResponse) {}
