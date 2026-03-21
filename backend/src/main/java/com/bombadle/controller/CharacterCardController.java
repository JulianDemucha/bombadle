package com.bombadle.controller;

import com.bombadle.dto.CharacterCardSearchDto;
import com.bombadle.entity.CharacterCard;
import com.bombadle.service.game.CharacterCardService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/character-card")
@AllArgsConstructor
public class CharacterCardController {
    private final CharacterCardService characterCardService;

//    @GetMapping("/{id}")
//    public CharacterCard getCharacterCardById(@PathVariable Long id) {
//        return characterCardService.findCharacterCardById(id);
//    }

    @GetMapping("/search-index")
    public List<CharacterCardSearchDto> searchCharacterCards() {
        return characterCardService.getAllCardsForSearch();
    }
}
