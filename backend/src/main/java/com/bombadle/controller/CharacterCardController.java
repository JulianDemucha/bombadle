package com.bombadle.controller;

import com.bombadle.dto.CardMatcher;
import com.bombadle.entity.CharacterCard;
import com.bombadle.service.CharacterCardService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/character-cards")
@AllArgsConstructor
public class CharacterCardController {
    private final CharacterCardService characterCardService;


    /* returns a list of enums - for example:
     [ MATCH, NOT_MATCH, MATCH, NOT_FULL_MATCH, NOT_MATCH ]
     */
    @GetMapping("/compare/{id}")
    public CardMatcher.FieldMatcher[] compareCard(@PathVariable Long id) {
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
