package com.bombadle.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClassicGuessAttempt.class, name = "CLASSIC"),
        @JsonSubTypes.Type(value = NameOnlyGuessAttempt.class, name = "QUOTES"),
        @JsonSubTypes.Type(value = NameOnlyGuessAttempt.class, name = "IMAGES")
})
public sealed interface GuessAttempt permits ClassicGuessAttempt, NameOnlyGuessAttempt, QuotesStageOneAttempt {
    @JsonIgnore
    boolean isCorrect();
}