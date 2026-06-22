package com.bombadle.dto;

import java.util.UUID;

public record AnonymousQuoteGameStateDto(
        QuotesGameStateDto gameState,
        UUID anonymousSessionId
)
{}
