package com.bombadle.service.game.core;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            ClassicGuessAttempt classicResult = assertInstanceOf(ClassicGuessAttempt.class, result);
            assertEquals(MatchType.MATCH, classicResult.name().match());
            assertEquals(MatchType.MATCH, classicResult.gender().match());
            assertEquals(MatchType.MATCH, classicResult.race().match());
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
            ClassicGuessAttempt classicResult = assertInstanceOf(ClassicGuessAttempt.class, result);
            assertEquals(MatchType.NOT_MATCH, classicResult.name().match());
            assertEquals(MatchType.NOT_MATCH, classicResult.gender().match());
            assertEquals(MatchType.NOT_MATCH, classicResult.race().match());
        }

        @Test
        void compareCharacterCards_quotesStage1_throwsIllegalArgumentException() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                    cardMatchingService.compareCharacterCards(guess, target, GameMode.QUOTES_STAGE_1)
            );
        }

        @Test
        void compareCharacterCards_quotesStage2_returnsNameOnlyMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);

            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(1L);
            when(guess.getName()).thenReturn("Name");

            // Act
            GuessAttempt result = cardMatchingService.compareCharacterCards(guess, target, GameMode.QUOTES_STAGE_2);

            // Assert
            NameOnlyGuessAttempt nameOnlyResult = assertInstanceOf(NameOnlyGuessAttempt.class, result);
            assertEquals(MatchType.MATCH, nameOnlyResult.name().match());
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
            NameOnlyGuessAttempt nameOnlyResult = assertInstanceOf(NameOnlyGuessAttempt.class, result);
            assertEquals(MatchType.NOT_MATCH, nameOnlyResult.name().match());
        }
    }
}