package com.bombadle.dto.request;

import java.util.List;

public record AdminQuoteRequest(
        String quoteBeginning,
        List<String> options,
        String correctAnswer,
        String target,
        Integer appearanceEpisode
) {
}
