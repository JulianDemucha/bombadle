package com.bombadle.service.game;

import com.bombadle.dto.CardField;
import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.NameOnlyGuessAttempt;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CardMatchingService {
    private final MatchUtils matchUtils;

    @Cacheable(value = "character-card-compare", key = "#guess.id + '-' + #targetCharacterCard.id + '-' + #gameMode")
    public GuessAttempt compareCharacterCards(CharacterCard guess, CharacterCard targetCharacterCard, GameMode gameMode) {
        return switch (gameMode) {
            case CLASSIC -> compareFull(guess, targetCharacterCard);
            case QUOTES, IMAGES -> compareNameOnly(guess, targetCharacterCard);
        };
    }

    /*
        todo:
         In the future, consider situations where guess card has
         all matches for today card, but also some more (currently,
         in that specific circumstances it returns NOT_FULL_MATCH,
         which might also be okay because it will be displayed as yellow
         in the frontend and can be included in the color legend).
    */
    // Name Gender Race Alive Affiliations FirstAppearanceEpisode
    private ClassicGuessAttempt compareFull(CharacterCard guess, CharacterCard targetCharacterCard) {

        if (targetCharacterCard.getId().equals(guess.getId())) {
            return getFullMatch(guess);
        }

        return ClassicGuessAttempt.builder()
                .name(new CardField<>(guess.getName(), MatchType.NOT_MATCH))
                .gender(matchUtils.getMatch(
                        guess.getGender(),
                        targetCharacterCard.getGender(),
                        this::checkGender
                ))
                .race(matchUtils.getMatch(
                        guess.getRace(),
                        targetCharacterCard.getRace(),
                        this::checkRace
                ))
                .alive(matchUtils.getMatch(
                        guess.getAlive(),
                        targetCharacterCard.getAlive(),
                        this::checkAlive
                ))
                .colors(matchUtils.getMatch(
                        guess.getColors(),
                        targetCharacterCard.getColors(),
                        this::checkColors
                ))
                .affiliations(matchUtils.getMatch(
                        guess.getAffiliations(),
                        targetCharacterCard.getAffiliations(),
                        this::checkAffiliations
                ))
                .firstAppearanceEpisode(matchUtils.getMatch(
                        guess.getFirstAppearanceEpisode(),
                        targetCharacterCard.getFirstAppearanceEpisode(),
                        this::checkFirstAppearanceEpisode
                ))
                .build();
    }

    private NameOnlyGuessAttempt compareNameOnly(CharacterCard guess, CharacterCard targetCharacterCard) {
        MatchType nameMatch = checkId(guess.getId(), targetCharacterCard.getId());

        return NameOnlyGuessAttempt.builder()
                .name(new CardField<>(guess.getName(), nameMatch))
                .build();
    }

    private MatchType checkId(Long id, Long targetId) {
        return id.equals(targetId) ?
                MatchType.MATCH : MatchType.NOT_MATCH;
    }

    private MatchType checkGender(Gender gender, Gender targetGender) {
        return gender.equals(targetGender) ?
                MatchType.MATCH : MatchType.NOT_MATCH;
    }

    private MatchType checkRace(Race race, Race targetRace) {
        return race.equals(targetRace) ?
                MatchType.MATCH : MatchType.NOT_MATCH;
    }

    private MatchType checkAlive(boolean alive, boolean targetAlive) {
        return alive == targetAlive ? MatchType.MATCH : MatchType.NOT_MATCH;
    }

    private MatchType checkColors(Set<Color> guessColors, Set<Color> targetColors) {
        return matchUtils.checkSetMatch(guessColors, targetColors);
    }

    private MatchType checkAffiliations(Set<Affiliation> guessAffiliations, Set<Affiliation> targetAffiliations) {
        return matchUtils.checkSetMatch(guessAffiliations, targetAffiliations);
    }

    private MatchType checkFirstAppearanceEpisode(Integer firstAppearanceEpisode, Integer targetFirstAppearanceEpisode) {
        return switch (firstAppearanceEpisode) {
            case Integer fae when
                    (fae.equals(targetFirstAppearanceEpisode)) -> MatchType.MATCH;

            case Integer fae when
                    (fae > targetFirstAppearanceEpisode) -> MatchType.LOWER;

            default -> MatchType.HIGHER;
        };
    }

    private ClassicGuessAttempt getFullMatch(CharacterCard guess) {
        return ClassicGuessAttempt.builder()
                .name(
                        matchUtils.getManualMatch(guess.getName(), MatchType.MATCH)
                )
                .gender(
                        matchUtils.getManualMatch(guess.getGender(), MatchType.MATCH)
                )
                .race(
                        matchUtils.getManualMatch(guess.getRace(), MatchType.MATCH)
                )
                .alive(
                        matchUtils.getManualMatch(guess.getAlive(), MatchType.MATCH)
                )
                .colors(
                        matchUtils.getManualMatch(guess.getColors(), MatchType.MATCH)
                )
                .affiliations(
                        matchUtils.getManualMatch(guess.getAffiliations(), MatchType.MATCH)
                )
                .firstAppearanceEpisode(
                        matchUtils.getManualMatch(guess.getFirstAppearanceEpisode(), MatchType.MATCH)
                )
                .build();
    }



}
