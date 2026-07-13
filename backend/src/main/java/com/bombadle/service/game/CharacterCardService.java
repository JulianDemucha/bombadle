package com.bombadle.service.game;

import com.bombadle.dto.CharacterCardSearchDto;
import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CharacterCardService {
    private static final Logger log = LoggerFactory.getLogger(CharacterCardService.class);
    private final CharacterCardRepository repo;

    public Optional<CharacterCard> findCharacterCardById(Long id) {
        return repo.findById(id);
    }

    @Cacheable(value="search-index")
    public List<CharacterCardSearchDto> getAllCardsForSearch() {
        return CharacterCardSearchDto.toDto(repo.findAll());
    }

    public CharacterCard findRandomCard() {
        return repo.findRandomCard();
    }

    public CharacterCard findRandomCardExcluding(List<Long> excludedIds) {
        return repo.findRandomCardExcluding(excludedIds);
    }

}
