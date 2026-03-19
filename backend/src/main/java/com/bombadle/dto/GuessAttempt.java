package com.bombadle.dto;

import com.bombadle.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.Set;

@Builder
public record GuessAttempt(
        CardField<String> name,
        CardField<Gender> gender,
        CardField<Race> race,
        CardField<Boolean> alive,
        CardField<Set<Color>> colors,
        CardField<Set<Affiliation>> affiliations,
        CardField<Integer> firstAppearanceEpisode

) {
    @JsonIgnore
    public boolean isCorrect() {
        return name.match().equals(MatchType.MATCH);
    }
}