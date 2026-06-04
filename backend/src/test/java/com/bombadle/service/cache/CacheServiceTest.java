package com.bombadle.service.cache;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.MatchUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @InjectMocks
    private CacheService cacheService;

    @Mock
    private CharacterCardRepository characterCardRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private MatchUtils matchUtils;

    @Mock
    private CurrentCharacterCardWrapper currentCharacterCardWrapper;

    @Mock
    private CharacterCardService characterCardService;

    @Mock
    private Cache cache;

    @Nested
    class ReloadCardCompareCacheTests {

        @Test
        void reloadCardCompareCache_iteratesOverAllCardsAndCompares() {
            // Arrange
            CharacterCard currentCard = CharacterCard.builder().id(1L).name("Current").build();
            CharacterCard card1 = CharacterCard.builder().id(2L).name("Card1").build();
            CharacterCard card2 = CharacterCard.builder().id(3L).name("Card2").build();

            when(currentCharacterCardWrapper.get()).thenReturn(currentCard);
            when(characterCardRepository.findAll()).thenReturn(List.of(card1, card2));

            // Act
            cacheService.reloadCardCompareCache();

            // Assert
            verify(matchUtils).compareCharacterCards(card1, currentCard);
            verify(matchUtils).compareCharacterCards(card2, currentCard);
            verifyNoMoreInteractions(matchUtils);
        }
    }

    @Nested
    class ReloadSearchIndexCacheTests {

        @Test
        void reloadSearchIndexCache_callsServiceToGetAllCards() {
            // Act
            cacheService.reloadSearchIndexCache();

            // Assert
            verify(characterCardService).getAllCardsForSearch();
        }
    }

    @Nested
    class EvictCacheTests {

        @Test
        void evictCache_validCacheNameAndCacheExists_clearsCache() {
            // Arrange
            String cacheName = "test-cache";
            when(cacheManager.getCache(cacheName)).thenReturn(cache);

            // Act
            cacheService.evictCache(cacheName);

            // Assert
            verify(cacheManager).getCache(cacheName);
            verify(cache).clear();
        }

        @Test
        void evictCache_validCacheNameButCacheDoesNotExist_doesNothing() {
            // Arrange
            String cacheName = "missing-cache";
            when(cacheManager.getCache(cacheName)).thenReturn(null);

            // Act
            cacheService.evictCache(cacheName);

            // Assert
            verify(cacheManager).getCache(cacheName);
            verifyNoInteractions(cache);
        }

        @Test
        void evictCache_cacheNameIsNull_doesNothing() {
            // Act
            cacheService.evictCache(null);

            // Assert
            verifyNoInteractions(cacheManager);
        }

        @Test
        void evictCache_cacheNameIsBlank_doesNothing() {
            // Act
            cacheService.evictCache("   ");

            // Assert
            verifyNoInteractions(cacheManager);
        }

        @Test
        void clear_callsEvictCacheSuccessfully() {
            // Arrange
            String cacheName = "clear-cache";
            when(cacheManager.getCache(cacheName)).thenReturn(cache);

            // Act
            cacheService.clear(cacheName);

            // Assert
            verify(cacheManager).getCache(cacheName);
            verify(cache).clear();
        }
    }

    @Nested
    class EvictCacheEntryTests {

        @Test
        void evictCacheEntry_validInputsAndCacheExists_evictsKey() {
            // Arrange
            String cacheName = "entry-cache";
            String key = "test-key";
            when(cacheManager.getCache(cacheName)).thenReturn(cache);

            // Act
            cacheService.evictCacheEntry(cacheName, key);

            // Assert
            verify(cacheManager).getCache(cacheName);
            verify(cache).evict(key);
        }

        @Test
        void evictCacheEntry_validInputsButCacheDoesNotExist_doesNothing() {
            // Arrange
            String cacheName = "missing-entry-cache";
            Object key = 123L;
            when(cacheManager.getCache(cacheName)).thenReturn(null);

            // Act
            cacheService.evictCacheEntry(cacheName, key);

            // Assert
            verify(cacheManager).getCache(cacheName);
            verifyNoInteractions(cache);
        }

        @Test
        void evictCacheEntry_cacheNameIsNull_doesNothing() {
            // Act
            cacheService.evictCacheEntry(null, "key");

            // Assert
            verifyNoInteractions(cacheManager);
        }

        @Test
        void evictCacheEntry_cacheNameIsBlank_doesNothing() {
            // Act
            cacheService.evictCacheEntry("  ", "key");

            // Assert
            verifyNoInteractions(cacheManager);
        }
    }

    @Nested
    class EvictAllCachesTests {

        @Test
        void evictAllCaches_iteratesAndClearsAllExistingCaches() {
            // Arrange
            Cache cache1 = mock(Cache.class);
            Cache cache2 = mock(Cache.class);

            when(cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2", "missing-cache"));
            when(cacheManager.getCache("cache1")).thenReturn(cache1);
            when(cacheManager.getCache("cache2")).thenReturn(cache2);
            when(cacheManager.getCache("missing-cache")).thenReturn(null);

            // Act
            cacheService.evictAllCaches();

            // Assert
            verify(cache1).clear();
            verify(cache2).clear();
        }

        @Test
        void evictAllCaches_noCachesExist_doesNothing() {
            // Arrange
            when(cacheManager.getCacheNames()).thenReturn(List.of());

            // Act
            cacheService.evictAllCaches();

            // Assert
            verify(cacheManager).getCacheNames();
            verifyNoMoreInteractions(cacheManager);
            verifyNoInteractions(cache);
        }
    }
}