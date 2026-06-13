package com.bombadle.service.cache;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.game.CardMatchingService;
import com.bombadle.service.game.CharacterCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final CharacterCardRepository characterCardRepository;
    private final CacheManager cacheManager;
    private final CardMatchingService cardMatchingService;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;
    private final CharacterCardService characterCardService;

    @CacheEvict(value = "character-card-compare", allEntries = true, beforeInvocation = true)
    public void reloadCardCompareCache() {
        List<CharacterCard> allCards = characterCardRepository.findAll();

        for (GameMode mode : GameMode.values()) {
            CharacterCard currentCardForMode = currentCharacterCardWrapper.get(mode);

            allCards.forEach(guessCard -> {
                cardMatchingService.compareCharacterCards(guessCard, currentCardForMode, mode);
            });
        }
    }

    @CacheEvict(value = "search-index")
    public void reloadSearchIndexCache() {
        characterCardService.getAllCardsForSearch();
    }

    public void evictCache(String cacheName) {
        if (cacheName == null || cacheName.isBlank()) {
            return;
        }
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public void clear(String cacheName) {
        evictCache(cacheName);
    }

    public void evictCacheEntry(String cacheName, Object key) {
        if (cacheName == null || cacheName.isBlank()) {
            return;
        }
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    public void evictAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}