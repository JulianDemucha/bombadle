package com.bombadle.service.game.core;

import com.bombadle.dto.CardField;
import com.bombadle.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class MatchUtils {

    public <T> CardField<T> getMatch(T fieldValue, T targetValue, BiFunction<T, T, MatchType> matcher) {
        MatchType matchResult = matcher.apply(fieldValue, targetValue);
        return CardField.<T>builder()
                .value(fieldValue)
                .match(matchResult)
                .build();
    }


    public <T> CardField<T> getManualMatch(T value, MatchType match) {
        return CardField.<T>builder()
                .value(value)
                .match(match)
                .build();
    }

    // SET HELPER
    public <T> MatchType checkSetMatch(Set<T> guessSet, Set<T> currentCardSet) {

        return switch (guessSet) {
            case Set<T> gs when
                    ((gs.containsAll(currentCardSet)) && currentCardSet.containsAll(gs)) -> MatchType.MATCH;

            case Set<T> gs when
                    (Collections.disjoint(gs, currentCardSet)) -> MatchType.NOT_MATCH;

            default -> MatchType.NOT_FULL_MATCH;
        };
    }
}
