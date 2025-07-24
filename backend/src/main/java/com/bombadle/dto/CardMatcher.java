package com.bombadle.dto;

import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.Affiliation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class CardMatcher {
    CharacterCard characterCard;

    //higher and lower are made for firstapperanceepisode
    //not full match is made for Affilations
    public enum FieldMatcher {
        MATCH,
        HIGHER,
        LOWER,
        NOT_MATCH,
        NOT_FULL_MATCH,
    }

    /*
        In the future, I might add situations where Guess has all matches for today,
         but some still exist (currently it returns NOT_FULL_MATCH,
          which is also okay because it will be displayed as yellow in the
          front panel and can be included in the color legend).
     */

    private FieldMatcher checkAffiliationsMatch(CharacterCard guess) {
        Set<Affiliation> today = this.characterCard.getAffiliations();
        Set<Affiliation> other = guess.getAffiliations();

        if (today.containsAll(other) && other.containsAll(today)) {
            return FieldMatcher.MATCH;
        }

        if (Collections.disjoint(today, other)) {
            return FieldMatcher.NOT_MATCH;
        }

        return FieldMatcher.NOT_FULL_MATCH;
    }

    private FieldMatcher checkFirstAppearanceEpisodeMatch(CharacterCard guess) {
        if (this.characterCard.getFirstAppearanceEpisode() == guess.getFirstAppearanceEpisode()) {
            return CardMatcher.FieldMatcher.MATCH;
        }

        if (this.characterCard.getFirstAppearanceEpisode() > guess.getFirstAppearanceEpisode()) {
            return CardMatcher.FieldMatcher.HIGHER;
        }

        return CardMatcher.FieldMatcher.LOWER;
    }

    // Name Race Alive Affilations FirstApperanceEpisode
    public FieldMatcher[] compareCharacterCards(CharacterCard guess) {
        if (this.characterCard.getId() == guess.getId()) {
            return new FieldMatcher[]{FieldMatcher.MATCH, FieldMatcher.MATCH
                    , FieldMatcher.MATCH, FieldMatcher.MATCH, FieldMatcher.MATCH};
        }

        return new FieldMatcher[]{
                (this.characterCard.getName() == guess.getName() ? FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (this.characterCard.getRace() == guess.getRace() ? FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (this.characterCard.getAlive() == guess.getAlive() ? FieldMatcher.MATCH : FieldMatcher.NOT_MATCH),
                (checkFirstAppearanceEpisodeMatch(guess)),
                (checkAffiliationsMatch(guess))
        };

    }

    public void refreshCharacterCard(CharacterCard newTodayCard) {
        this.characterCard = newTodayCard;
    }

}
