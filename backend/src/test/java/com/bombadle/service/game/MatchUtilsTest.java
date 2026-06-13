package com.bombadle.service.game;

import com.bombadle.dto.CardField;
import com.bombadle.enums.MatchType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class MatchUtilsTest {

    @InjectMocks
    private MatchUtils matchUtils;

    @Nested
    class GetMatchTests {

        @Test
        void getMatch_validMatcher_returnsCardFieldWithCalculatedMatch() {
            // Arrange
            String fieldValue = "Test1";
            String targetValue = "Test2";
            BiFunction<String, String, MatchType> matcher = (val1, val2) -> MatchType.NOT_MATCH;

            // Act
            CardField<String> result = matchUtils.getMatch(fieldValue, targetValue, matcher);

            // Assert
            assertEquals(fieldValue, result.value());
            assertEquals(MatchType.NOT_MATCH, result.match());
        }
    }

    @Nested
    class GetManualMatchTests {

        @Test
        void getManualMatch_validInputs_returnsCardFieldWithGivenMatch() {
            // Arrange
            String value = "TestValue";
            MatchType matchType = MatchType.MATCH;

            // Act
            CardField<String> result = matchUtils.getManualMatch(value, matchType);

            // Assert
            assertEquals(value, result.value());
            assertEquals(matchType, result.match());
        }
    }

    @Nested
    class CheckSetMatchTests {

        @Test
        void checkSetMatch_setsAreIdentical_returnsMatch() {
            // Arrange
            Set<String> guessSet = Set.of("A", "B");
            Set<String> currentCardSet = Set.of("A", "B");

            // Act
            MatchType result = matchUtils.checkSetMatch(guessSet, currentCardSet);

            // Assert
            assertEquals(MatchType.MATCH, result);
        }

        @Test
        void checkSetMatch_setsAreDisjoint_returnsNotMatch() {
            // Arrange
            Set<String> guessSet = Set.of("A", "B");
            Set<String> currentCardSet = Set.of("C", "D");

            // Act
            MatchType result = matchUtils.checkSetMatch(guessSet, currentCardSet);

            // Assert
            assertEquals(MatchType.NOT_MATCH, result);
        }

        @Test
        void checkSetMatch_setsOverlap_returnsNotFullMatch() {
            // Arrange
            Set<String> guessSet = Set.of("A", "B");
            Set<String> currentCardSet = Set.of("B", "C");

            // Act
            MatchType result = matchUtils.checkSetMatch(guessSet, currentCardSet);

            // Assert
            assertEquals(MatchType.NOT_FULL_MATCH, result);
        }
    }
}