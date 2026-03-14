package com.bombadle.controller;

import com.bombadle.service.CardMatchingService;
import com.bombadle.entity.CharacterCard;
import com.bombadle.service.CharacterCardService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/character-card")
@AllArgsConstructor
public class CharacterCardController {
    private final CharacterCardService characterCardService;


    /* returns an array of CardField - for example:
     [
     (name_value, MATCH),
     (gender_value, NOT_MATCH),
     (race_value, MATCH),
     (alive_value, NOT_MATCH),
     (affiliations_value, NOT_FULL_MATCH),
     (first_episode_value, HIGHER)
     ]
     */

    @GetMapping("/compare/{id}")
    public CardMatchingService.CardField<?>[] compareCard(@PathVariable Long id) {
        CharacterCard characterCard = characterCardService.findCharacterCardById(id);
        if (characterCard != null)
            return characterCardService.compareCharacterCard(characterCard);
        else return null;
    }

    @GetMapping("/{id}")
    public CharacterCard getCharacterCardById(@PathVariable Long id) {
        return characterCardService.findCharacterCardById(id);
    }

    @GetMapping("/currentCharacterCard")
    public CharacterCard getCurrentCharacterCard() {
        return characterCardService.getCurrentCharacterCard();
    }
}
