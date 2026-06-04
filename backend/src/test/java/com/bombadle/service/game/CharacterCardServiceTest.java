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
        void getAllCardsForSearch_cardsExist_returnsListOfCards() {
            // Arrange
            CharacterCardSearchDto dto = mock(CharacterCardSearchDto.class);
            List<CharacterCardSearchDto> expectedList = List.of(dto);
            when(repo.findAllCardsForSearch()).thenReturn(expectedList);

            // Act
            List<CharacterCardSearchDto> result = characterCardService.getAllCardsForSearch();

            // Assert
            assertEquals(expectedList, result);
            assertEquals(1, result.size());
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
}