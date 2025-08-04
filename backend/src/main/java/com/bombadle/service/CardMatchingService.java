package com.bombadle.service;

import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.Affiliation;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Getter
@Service
public class CardMatchingService {
    CharacterCard currentCharacterCard;

    //higher and lower are made for firstAppearanceEpisode
    //not full match is made for Affiliations
    public enum FieldMatcher {
        MATCH,
        HIGHER,
        LOWER,
        NOT_MATCH,
        NOT_FULL_MATCH,
    }

    /*  todo (optional)
        In the future, consider situations where guess card has
        all matches for today card, but also some more (currently,
        in that specific circumstances it returns NOT_FULL_MATCH,
        which is also okay because it will be displayed as yellow
        in the frontend and can be included in the color legend).
     */

    private FieldMatcher checkAffiliationsMatch(CharacterCard guess) {
        Set<Affiliation> today = this.currentCharacterCard.getAffiliations();
        Set<Affiliation> other = guess.getAffiliations();

        // full affiliations match
        if (today.containsAll(other) && other.containsAll(today)) {
            return FieldMatcher.MATCH;
        }

        // not even one common affiliation between the two lists
        if (Collections.disjoint(today, other)) {
            return FieldMatcher.NOT_MATCH;
        }

        return FieldMatcher.NOT_FULL_MATCH;
    }

    private FieldMatcher checkFirstAppearanceEpisodeMatch(CharacterCard guess) {
        if (this.currentCharacterCard.getFirstAppearanceEpisode()
                == guess.getFirstAppearanceEpisode()) {
            return CardMatchingService.FieldMatcher.MATCH;
        }

        if (this.currentCharacterCard.getFirstAppearanceEpisode()
                > guess.getFirstAppearanceEpisode()) {
            return CardMatchingService.FieldMatcher.HIGHER;
        }

        return CardMatchingService.FieldMatcher.LOWER;
    }

    // Name Gender Race Alive Affiliations FirstAppearanceEpisode
    public FieldMatcher[] compareCharacterCards(CharacterCard guess) {
        if (this.currentCharacterCard.equals(guess)) {
            return new FieldMatcher[]{
                    FieldMatcher.MATCH, FieldMatcher.MATCH, FieldMatcher.MATCH
                    , FieldMatcher.MATCH, FieldMatcher.MATCH, FieldMatcher.MATCH
                    , FieldMatcher.MATCH
            };
        }

        return new FieldMatcher[]{
                (this.currentCharacterCard.getName().equals(guess.getName()) ?
                        FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (this.currentCharacterCard.getGender() == guess.getGender() ?
                        FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (this.currentCharacterCard.getRace() == guess.getRace() ?
                        FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (this.currentCharacterCard.getAlive() == guess.getAlive() ?
                        FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (checkFirstAppearanceEpisodeMatch(guess)),
                (checkAffiliationsMatch(guess))
        };

    }

    public void refreshCharacterCard(CharacterCard newTodayCard) {
        this.currentCharacterCard = newTodayCard;
    }

}
