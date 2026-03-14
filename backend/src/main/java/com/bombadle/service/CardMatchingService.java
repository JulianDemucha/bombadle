package com.bombadle.service;

import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Gender;
import com.bombadle.enums.Race;
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
    public enum FieldMatch {
        MATCH,
        HIGHER,
        LOWER,
        NOT_MATCH,
        NOT_FULL_MATCH,
    }

    public record CardField<T>(T value, FieldMatch match){}

    /*  todo (optional)
        In the future, consider situations where guess card has
        all matches for today card, but also some more (currently,
        in that specific circumstances it returns NOT_FULL_MATCH,
        which is also okay because it will be displayed as yellow
        in the frontend and can be included in the color legend).
     */

    private CardField<Set<Affiliation>> checkAffiliationsMatch(Set<Affiliation> affiliations) {
        Set<Affiliation> today = this.currentCharacterCard.getAffiliations();

        // full affiliations match
        if (today.containsAll(affiliations) && affiliations.containsAll(today)) {
            return new CardField<>(affiliations, FieldMatch.MATCH);
        }

        // not even one common affiliation between the two lists
        if (Collections.disjoint(today, affiliations)) {
            return new CardField<>(affiliations, FieldMatch.NOT_MATCH);
        }

        return new CardField<>(affiliations, FieldMatch.NOT_FULL_MATCH);
    }

    private CardField<Integer> checkFirstAppearanceEpisodeMatch(int firstAppearanceEpisode) {
        if (this.currentCharacterCard.getFirstAppearanceEpisode()
                == firstAppearanceEpisode) {
            return new CardField<>(firstAppearanceEpisode, FieldMatch.MATCH);
        }

        if (this.currentCharacterCard.getFirstAppearanceEpisode()
                > firstAppearanceEpisode) {
            return new CardField<>(firstAppearanceEpisode, FieldMatch.HIGHER);
        }

        return new CardField<>(firstAppearanceEpisode, FieldMatch.LOWER);
    }

    // Name Gender Race Alive Affiliations FirstAppearanceEpisode
    public CardField<?>[] compareCharacterCards(CharacterCard guess) {
        String name = guess.getName();
        Gender gender = guess.getGender();
        Race race = guess.getRace();
        Boolean alive = guess.getAlive();
        Set<Affiliation> affiliations = guess.getAffiliations();
        int firstAppearanceEpisode = guess.getFirstAppearanceEpisode();

        if (this.currentCharacterCard.equals(guess)) {
            return new CardField[]{
                    new CardField<>(name, FieldMatch.MATCH),new CardField<>(gender, FieldMatch.MATCH),
                    new CardField<>(race, FieldMatch.MATCH), new CardField<>(alive, FieldMatch.MATCH),
                    new CardField<>(affiliations, FieldMatch.MATCH),
                    new CardField<>(firstAppearanceEpisode, FieldMatch.MATCH)
            };
        }

        return new CardField[]{
                new CardField<>(name, FieldMatch.NOT_MATCH),
                new CardField<>(gender,(this.currentCharacterCard.getGender() == gender ?
                        FieldMatch.MATCH : FieldMatch.NOT_MATCH)),
                new CardField<>(race,(this.currentCharacterCard.getRace() == race ?
                        FieldMatch.MATCH : FieldMatch.NOT_MATCH)),
                new CardField<>(alive,(this.currentCharacterCard.getAlive() == alive ?
                        FieldMatch.MATCH : FieldMatch.NOT_MATCH)),
                (checkFirstAppearanceEpisodeMatch(guess.getFirstAppearanceEpisode())),
                (checkAffiliationsMatch(guess.getAffiliations()))
        };

    }

    public void refreshCharacterCard(CharacterCard newTodayCard) {
        this.currentCharacterCard = newTodayCard;
    }

}
