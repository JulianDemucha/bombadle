package com.bombadle.dto;

import java.util.List;

public record QuotesGameStateDto(
        QuotePromptDto prompt,
        List<QuotesStageOneAttempt> stageOneGuesses,
        List<NameOnlyGuessAttempt> stageTwoGuesses,
        boolean isStageOnePassed,
        boolean isStageTwoPassed
) {}
