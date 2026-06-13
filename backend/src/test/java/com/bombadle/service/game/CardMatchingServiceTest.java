package com.bombadle.service.game;

import com.bombadle.dto.CardField;
import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.NameOnlyGuessAttempt;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.MatchType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardMatchingServiceTest {

    @Mock
    private MatchUtils matchUtils;

    @InjectMocks
    private CardMatchingService cardMatchingService;

    @Nested
    class CompareCharacterCardsTests {

        @Test
        void compareCharacterCards_classicModeIdsEqual_returnsFullMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);

            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(1L);
            when(guess.getName()).thenReturn("Name");
            when(matchUtils.getManualMatch(any(), any())).thenReturn(new CardField<>("Value", MatchType.MATCH));

            // Act
            GuessAttempt result = cardMatchingService.compareCharacterCards(guess, target, GameMode.CLASSIC);

            // Assert
            assertInstanceOf(ClassicGuessAttempt.class, result);
            assertEquals(MatchType.MATCH, result.name().match());
            assertEquals(MatchType.MATCH, ((ClassicGuessAttempt) result).gender().match());
            assertEquals(MatchType.MATCH, ((ClassicGuessAttempt) result).race().match());
        }

        @Test
        void compareCharacterCards_classicModeIdsNotEqual_returnsCalculatedMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);

            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(guess.getName()).thenReturn("GuessName");
            when(matchUtils.getMatch(any(), any(), any())).thenReturn(new CardField<>("Value", MatchType.NOT_MATCH));

            // Act
            GuessAttempt result = cardMatchingService.compareCharacterCards(guess, target, GameMode.CLASSIC);

            // Assert
            assertInstanceOf(ClassicGuessAttempt.class, result);
            assertEquals(MatchType.NOT_MATCH, result.name().match());
            assertEquals(MatchType.NOT_MATCH, ((ClassicGuessAttempt) result).gender().match());
            assertEquals(MatchType.NOT_MATCH, ((ClassicGuessAttempt) result).race().match());
        }

        @Test
        void compareCharacterCards_quotesMode_returnsNameOnlyMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);

            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(1L);
            when(guess.getName()).thenReturn("Name");

            // Act
            GuessAttempt result = cardMatchingService.compareCharacterCards(guess, target, GameMode.QUOTES);

            // Assert
            assertInstanceOf(NameOnlyGuessAttempt.class, result);
            assertEquals(MatchType.MATCH, result.name().match());
        }

        @Test
        void compareCharacterCards_imagesMode_returnsNameOnlyNotMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);

            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(guess.getName()).thenReturn("GuessName");

            // Act
            GuessAttempt result = cardMatchingService.compareCharacterCards(guess, target, GameMode.IMAGES);

            // Assert
            assertInstanceOf(NameOnlyGuessAttempt.class, result);
            assertEquals(MatchType.NOT_MATCH, result.name().match());
        }
    }
}