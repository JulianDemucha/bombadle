package com.bombadle.service.cache;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final CharacterCardRepository characterCardRepository;
    private final CacheManager cacheManager;
    private final MatchUtils matchUtils;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;
    private final CharacterCardService characterCardService;

    @CacheEvict(value = "character-card-compare", allEntries = true, beforeInvocation = true)
    public void reloadCardCompareCache() {
        CharacterCard currentCharacterCard = currentCharacterCardWrapper.get();
        characterCardRepository.findAll().forEach(cc -> {
            matchUtils.compareCharacterCards(cc, currentCharacterCard);
        });
    }

    @CacheEvict(value = "search-index")
    public void reloadSearchIndexCache() {
        characterCardService.getAllCardsForSearch();
    }
}
