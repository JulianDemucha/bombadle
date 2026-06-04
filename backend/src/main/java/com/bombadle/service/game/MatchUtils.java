package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.CardField;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class MatchUtils {

    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;

    /*
    todo:
     In the future, consider situations where guess card has
     all matches for today card, but also some more (currently,
     in that specific circumstances it returns NOT_FULL_MATCH,
     which might also be okay because it will be displayed as yellow
     in the frontend and can be included in the color legend).
     */

    public static GuessAttempt getFullMatch(CharacterCard guess) {
        return GuessAttempt.builder()
                .name(
                        getManualMatch(guess.getName(), MatchType.MATCH)
                )
                .gender(
                        getManualMatch(guess.getGender(), MatchType.MATCH)
                )
                .race(
                        getManualMatch(guess.getRace(), MatchType.MATCH)
                )
                .alive(
                        getManualMatch(guess.getAlive(), MatchType.MATCH)
                )
                .colors(
                        getManualMatch(guess.getColors(), MatchType.MATCH)
                )
                .affiliations(
                        getManualMatch(guess.getAffiliations(), MatchType.MATCH)
                )
                .firstAppearanceEpisode(
                        getManualMatch(guess.getFirstAppearanceEpisode(), MatchType.MATCH)
                )
                .build();
    }

    public static <T> CardField<T> getMatch(T fieldValue, T targetValue, BiFunction<T, T, MatchType> matcher) {
        MatchType matchResult = matcher.apply(fieldValue, targetValue);
        return CardField.<T>builder()
                .value(fieldValue)
                .match(matchResult)
                .build();
    }


    public static <T> CardField<T> getManualMatch(T value, MatchType match) {
        return CardField.<T>builder()
                .value(value)
                .match(match)
                .build();
    }


    // Name Gender Race Alive Affiliations FirstAppearanceEpisode
    public GuessAttempt compareCharacterCardClassic(CharacterCard guess) {
        CharacterCard currentCharacterCard = currentCharacterCardWrapper.get();
        return compareCharacterCards(guess, currentCharacterCard);
    }

    // Name Gender Race Alive Affiliations FirstAppearanceEpisode
    @Cacheable(value = "character-card-compare")
    public GuessAttempt compareCharacterCards(CharacterCard guess, CharacterCard targetCharacterCard) {

        if (targetCharacterCard.getId().equals(guess.getId())) {
            return getFullMatch(guess);
        }

        return GuessAttempt.builder()
                .name(new CardField<>(guess.getName(), MatchType.NOT_MATCH))
                .gender(getMatch(
                        guess.getGender(),
                        targetCharacterCard.getGender(),
                        this::checkGender
                ))
                .race(getMatch(
                        guess.getRace(),
                        targetCharacterCard.getRace(),
                        this::checkRace
                ))
                .alive(getMatch(
                        guess.getAlive(),
                        targetCharacterCard.getAlive(),
                        this::checkAlive
                ))
                .colors(getMatch(
                        guess.getColors(),
                        targetCharacterCard.getColors(),
                        this::checkColors
                ))
                .affiliations(getMatch(
                        guess.getAffiliations(),
                        targetCharacterCard.getAffiliations(),
                        this::checkAffiliations
                ))
                .firstAppearanceEpisode(getMatch(
                        guess.getFirstAppearanceEpisode(),
                        targetCharacterCard.getFirstAppearanceEpisode(),
                        this::checkFirstAppearanceEpisode
                ))
                .build();
    }

    public MatchType checkName(String name, String targetName) {
        return name.equals(targetName) ?
                MatchType.MATCH : MatchType.NOT_MATCH;
    }

    public MatchType checkGender(Gender gender, Gender targetGender) {
        return gender.equals(targetGender) ?
                MatchType.MATCH : MatchType.NOT_MATCH;
    }

    public MatchType checkRace(Race race, Race targetRace) {
        return race.equals(targetRace) ?
                MatchType.MATCH : MatchType.NOT_MATCH;
    }

    public MatchType checkAlive(boolean alive, boolean targetAlive) {
        return alive == targetAlive ? MatchType.MATCH : MatchType.NOT_MATCH;
    }

    public MatchType checkColors(Set<Color> guessColors, Set<Color> targetColors) {
        return checkSetMatch(guessColors, targetColors);
    }

    public MatchType checkAffiliations(Set<Affiliation> guessAffiliations, Set<Affiliation> targetAffiliations) {
        return checkSetMatch(guessAffiliations, targetAffiliations);
    }

    public MatchType checkFirstAppearanceEpisode(Integer firstAppearanceEpisode, Integer targetFirstAppearanceEpisode) {
        return switch (firstAppearanceEpisode) {
            case Integer fae when
                    (fae.equals(targetFirstAppearanceEpisode)) -> MatchType.MATCH;

            case Integer fae when
                    (fae > targetFirstAppearanceEpisode) -> MatchType.LOWER;

            default -> MatchType.HIGHER;
        };
    }

    // SET HELPER
    private <T> MatchType checkSetMatch(Set<T> guessSet, Set<T> currentCardSet) {

        return switch (guessSet) {
            case Set<T> gs when
                    ((gs.containsAll(currentCardSet)) && currentCardSet.containsAll(gs)) -> MatchType.MATCH;

            case Set<T> gs when
                    (Collections.disjoint(gs, currentCardSet)) -> MatchType.NOT_MATCH;

            default -> MatchType.NOT_FULL_MATCH;
        };
    }
}
