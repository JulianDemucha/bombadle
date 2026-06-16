package com.bombadle.service.cache;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.game.CardMatchingService;
import com.bombadle.service.game.CharacterCardService;
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
    private CardMatchingService cardMatchingService;

    @Mock
    private CurrentGameStateWrapper currentGameStateWrapper;

    @Mock
    private CharacterCardService characterCardService;

    @Mock
    private Cache cache;

    @Nested
    class ReloadCardCompareCacheTests {

        @Test
        void reloadCardCompareCache_iteratesOverAllCardsAndModesExceptQuotesStage1() {
            // ARRANGE
            CharacterCard currentCard = CharacterCard.builder().id(1L).name("Current").build();
            CharacterCard card1 = CharacterCard.builder().id(2L).name("Card1").build();
            CharacterCard card2 = CharacterCard.builder().id(3L).name("Card2").build();

            when(currentGameStateWrapper.getCard(any(GameMode.class))).thenReturn(currentCard);
            when(characterCardRepository.findAll()).thenReturn(List.of(card1, card2));

            // ACT
            cacheService.reloadCardCompareCache();

            // ASSERT
            for (GameMode mode : GameMode.values()) {
                if (mode == GameMode.QUOTES_STAGE_1) {
                    verify(cardMatchingService, never()).compareCharacterCards(any(), any(), eq(mode));
                    continue;
                }
                verify(cardMatchingService).compareCharacterCards(card1, currentCard, mode);
                verify(cardMatchingService).compareCharacterCards(card2, currentCard, mode);
            }
        }
    }

    @Nested
    class ReloadSearchIndexCacheTests {

        @Test
        void reloadSearchIndexCache_callsServiceToGetAllCards() {
            // ARRANGE
            // ACT
            cacheService.reloadSearchIndexCache();

            // ASSERT
            verify(characterCardService).getAllCardsForSearch();
        }
    }

    @Nested
    class EvictCacheTests {

        @Test
        void evictCache_validCacheNameAndCacheExists_clearsCache() {
            // ARRANGE
            String cacheName = "test-cache";
            when(cacheManager.getCache(cacheName)).thenReturn(cache);

            // ACT
            cacheService.evictCache(cacheName);

            // ASSERT
            verify(cacheManager).getCache(cacheName);
            verify(cache).clear();
        }

        @Test
        void evictCache_validCacheNameButCacheDoesNotExist_doesNothing() {
            // ARRANGE
            String cacheName = "missing-cache";
            when(cacheManager.getCache(cacheName)).thenReturn(null);

            // ACT
            cacheService.evictCache(cacheName);

            // ASSERT
            verify(cacheManager).getCache(cacheName);
            verifyNoInteractions(cache);
        }

        @Test
        void evictCache_cacheNameIsNull_doesNothing() {
            // ARRANGE
            // ACT
            cacheService.evictCache(null);

            // ASSERT
            verifyNoInteractions(cacheManager);
        }

        @Test
        void evictCache_cacheNameIsBlank_doesNothing() {
            // ARRANGE
            // ACT
            cacheService.evictCache("   ");

            // ASSERT
            verifyNoInteractions(cacheManager);
        }

        @Test
        void clear_callsEvictCacheSuccessfully() {
            // ARRANGE
            String cacheName = "clear-cache";
            when(cacheManager.getCache(cacheName)).thenReturn(cache);

            // ACT
            cacheService.clear(cacheName);

            // ASSERT
            verify(cacheManager).getCache(cacheName);
            verify(cache).clear();
        }
    }

    @Nested
    class EvictCacheEntryTests {

        @Test
        void evictCacheEntry_validInputsAndCacheExists_evictsKey() {
            // ARRANGE
            String cacheName = "entry-cache";
            String key = "test-key";
            when(cacheManager.getCache(cacheName)).thenReturn(cache);

            // ACT
            cacheService.evictCacheEntry(cacheName, key);

            // ASSERT
            verify(cacheManager).getCache(cacheName);
            verify(cache).evict(key);
        }

        @Test
        void evictCacheEntry_validInputsButCacheDoesNotExist_doesNothing() {
            // ARRANGE
            String cacheName = "missing-entry-cache";
            Object key = 123L;
            when(cacheManager.getCache(cacheName)).thenReturn(null);

            // ACT
            cacheService.evictCacheEntry(cacheName, key);

            // ASSERT
            verify(cacheManager).getCache(cacheName);
            verifyNoInteractions(cache);
        }

        @Test
        void evictCacheEntry_cacheNameIsNull_doesNothing() {
            // ARRANGE
            // ACT
            cacheService.evictCacheEntry(null, "key");

            // ASSERT
            verifyNoInteractions(cacheManager);
        }

        @Test
        void evictCacheEntry_cacheNameIsBlank_doesNothing() {
            // ARRANGE
            // ACT
            cacheService.evictCacheEntry("  ", "key");

            // ASSERT
            verifyNoInteractions(cacheManager);
        }
    }

    @Nested
    class EvictAllCachesTests {

        @Test
        void evictAllCaches_iteratesAndClearsAllExistingCaches() {
            // ARRANGE
            Cache cache1 = mock(Cache.class);
            Cache cache2 = mock(Cache.class);

            when(cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2", "missing-cache"));
            when(cacheManager.getCache("cache1")).thenReturn(cache1);
            when(cacheManager.getCache("cache2")).thenReturn(cache2);
            when(cacheManager.getCache("missing-cache")).thenReturn(null);

            // ACT
            cacheService.evictAllCaches();

            // ASSERT
            verify(cache1).clear();
            verify(cache2).clear();
        }

        @Test
        void evictAllCaches_noCachesExist_doesNothing() {
            // ARRANGE
            when(cacheManager.getCacheNames()).thenReturn(List.of());

            // ACT
            cacheService.evictAllCaches();

            // ASSERT
            verify(cacheManager).getCacheNames();
            verifyNoMoreInteractions(cacheManager);
            verifyNoInteractions(cache);
        }
    }
}