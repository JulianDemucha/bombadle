package com.bombadle.service;

import com.bombadle.dto.CardMatcher;
import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CharacterCardService {
    private static final Logger log = LoggerFactory.getLogger(CharacterCardService.class);
    private final CharacterCardRepository repo;
    private final CardMatcher cardMatcher;

    public CardMatcher.FieldMatcher[] compareCharacterCard(CharacterCard characterCard) {
        return cardMatcher.compareCharacterCards(characterCard);
    }

    public CharacterCard getCurrentCharacterCard() {
        return cardMatcher.getCurrentCharacterCard();
    }

    public CharacterCard findCharacterCardById(Long id) {
        return repo.findById(id).orElse(null);
    }

}
