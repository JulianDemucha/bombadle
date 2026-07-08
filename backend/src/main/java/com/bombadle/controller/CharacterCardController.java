package com.bombadle.controller;

import com.bombadle.dto.CharacterCardSearchDto;
import com.bombadle.dto.PreviousCharacterCardDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.CurrentCardStateService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/character-card")
@AllArgsConstructor
public class CharacterCardController {
    private final CharacterCardService characterCardService;
    private final CurrentCardStateService currentCardStateService;

    @GetMapping("/search-index")
    public List<CharacterCardSearchDto> searchCharacterCards() {
        return characterCardService.getAllCardsForSearch();
    }

    @GetMapping("/{gameMode}/previous-character-card")
    public ResponseEntity<PreviousCharacterCardDto> getPreviousCharacterCard(@PathVariable String gameMode) {
        GameMode mode = GameMode.valueOf(gameMode.toUpperCase());

        return currentCardStateService.getPreviousCharacterCard(mode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}