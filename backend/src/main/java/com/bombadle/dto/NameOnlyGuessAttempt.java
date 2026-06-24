package com.bombadle.dto;

import com.bombadle.enums.MatchType;
import lombok.Builder;

@Builder
public record NameOnlyGuessAttempt(
        CardField<String> name
) implements GuessAttempt {

    @Override
    public boolean isCorrect() {
        return name != null && MatchType.MATCH.equals(name.match());
    }
}