package com.bombadle.service.game;

import com.bombadle.dto.CharacterCardSearchDto;
import com.bombadle.entity.CharacterCard;
import com.bombadle.repository.CharacterCardRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterCardServiceTest {

    @Mock
    private CharacterCardRepository repo;

    @InjectMocks
    private CharacterCardService characterCardService;

    @Nested
    class FindCharacterCardByIdTests {

        @Test
        void findCharacterCardById_cardNotFound_returnsEmptyOptional() {
            // Arrange
            Long id = 1L;
            when(repo.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<CharacterCard> result = characterCardService.findCharacterCardById(id);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void findCharacterCardById_cardExists_returnsCardOptional() {
            // Arrange
            Long id = 1L;
            CharacterCard card = mock(CharacterCard.class);
            when(repo.findById(id)).thenReturn(Optional.of(card));

            // Act
            Optional<CharacterCard> result = characterCardService.findCharacterCardById(id);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(card, result.get());
        }
    }

    @Nested
    class GetAllCardsForSearchTests {

        @Test
        void getAllCardsForSearch_cardsExist_returnsMappedDtosIncludingAliases() {
            // Arrange
            CharacterCard card = mock(CharacterCard.class);
            when(card.getId()).thenReturn(1L);
            when(card.getName()).thenReturn("Kapitan Bomba");
            when(card.getImageSrc()).thenReturn("/images/1.jpg");
            when(card.getAliases()).thenReturn(Set.of("Kapitan Dupa"));
            when(repo.findAll()).thenReturn(List.of(card));

            // Act
            List<CharacterCardSearchDto> result = characterCardService.getAllCardsForSearch();

            // Assert
            assertEquals(1, result.size());
            CharacterCardSearchDto dto = result.get(0);
            assertEquals(1L, dto.id());
            assertEquals("Kapitan Bomba", dto.name());
            assertEquals("/images/1.jpg", dto.imageSrc());
            assertEquals(Set.of("Kapitan Dupa"), dto.aliases());
        }
    }

    @Test
    void findRandomCard_called_delegatesToRepository() {
        // Arrange
        CharacterCard card = mock(CharacterCard.class);
        when(repo.findRandomCard()).thenReturn(card);

        // Act
        CharacterCard result = characterCardService.findRandomCard();

        // Assert
        assertEquals(card, result);
        verify(repo).findRandomCard();
    }

    @Test
    void findRandomCardExcluding_called_delegatesToRepositoryWithSameExclusions() {
        // Arrange
        CharacterCard card = mock(CharacterCard.class);
        List<Long> excludedIds = List.of(1L, 2L);
        when(repo.findRandomCardExcluding(excludedIds)).thenReturn(card);

        // Act
        CharacterCard result = characterCardService.findRandomCardExcluding(excludedIds);

        // Assert
        assertEquals(card, result);
        verify(repo).findRandomCardExcluding(excludedIds);
    }
}