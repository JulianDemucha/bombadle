package com.bombadle.controller;

import com.bombadle.dto.CharacterCardSearchDto;
import com.bombadle.dto.PreviousCharacterCardDto;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.CurrentCardStateService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/previous-character-card")
    public ResponseEntity<PreviousCharacterCardDto> getPreviousCharacterCard() {
        CurrentCardState state = currentCardStateService.getCurrentCardState();
        CharacterCard prevCard = state.getPreviousCharacter();

        if (prevCard == null) {
            return ResponseEntity.noContent().build();
        }

        PreviousCharacterCardDto dto = PreviousCharacterCardDto.builder()
                .name(prevCard.getName())
                .imageSrc(prevCard.getImageSrc())
                .build();

        return ResponseEntity.ok(dto);
    }
}
