package com.bombadle.dto;

import com.bombadle.enums.*;
import lombok.Builder;
import java.util.Set;

@Builder
public record ClassicGuessAttempt(
        CardField<String> name,
        CardField<Gender> gender,
        CardField<Race> race,
        CardField<Boolean> alive,
        CardField<Set<Color>> colors,
        CardField<Set<Affiliation>> affiliations,
        CardField<Integer> firstAppearanceEpisode
) implements GuessAttempt {

    @Override
    public boolean isCorrect() {
        return name != null && MatchType.MATCH.equals(name.match());
    }
}