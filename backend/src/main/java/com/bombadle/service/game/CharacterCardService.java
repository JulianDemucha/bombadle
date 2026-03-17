package com.bombadle.service.game;

import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class CharacterCardService {
    private static final Logger log = LoggerFactory.getLogger(CharacterCardService.class);
    private final CharacterCardRepository repo;
    private final CardMatchingService cardMatcher;

    public Optional<CharacterCard> findCharacterCardById(Long id) {
        return repo.findById(id);
    }

}
