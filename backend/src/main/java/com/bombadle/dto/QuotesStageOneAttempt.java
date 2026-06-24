package com.bombadle.dto;

import com.bombadle.enums.MatchType;

public record QuotesStageOneAttempt(
        CardField<String> guess
) implements GuessAttempt{

    @Override
    public boolean isCorrect() {
        return guess != null && MatchType.MATCH.equals(guess.match());
    }
}
