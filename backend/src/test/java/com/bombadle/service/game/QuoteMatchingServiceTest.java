package com.bombadle.service.game;

import com.bombadle.dto.CardField;
import com.bombadle.dto.QuotesStageOneAttempt;
import com.bombadle.entity.Quote;
import com.bombadle.enums.MatchType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteMatchingServiceTest {

    @Mock
    private MatchUtils matchUtils;

    @InjectMocks
    private QuoteMatchingService quoteMatchingService;

    @Nested
    class GuessTests {

        @Test
        void guess_matchingQuote_returnsCorrectAttempt() {
            // ARRANGE
            String guess = "I am your father";
            String correctAnswer = "I am your father";
            Quote target = mock(Quote.class);
            when(target.getCorrectAnswer()).thenReturn(correctAnswer);

            when(matchUtils.getMatch(eq(guess), eq(correctAnswer), any())).thenAnswer(invocation -> {
                BiFunction<String, String, MatchType> comparator = invocation.getArgument(2);
                MatchType matchType = comparator.apply(guess, correctAnswer);
                return new CardField<>(guess, matchType);
            });

            // ACT
            QuotesStageOneAttempt result = quoteMatchingService.guess(guess, target);

            // ASSERT
            assertNotNull(result);
            assertEquals(MatchType.MATCH, result.guess().match());
            assertEquals(guess, result.guess().value());
            verify(matchUtils).getMatch(eq(guess), eq(correctAnswer), any());
        }

        @Test
        void guess_nonMatchingQuote_returnsIncorrectAttempt() {
            // ARRANGE
            String guess = "Luke, I am your father";
            String correctAnswer = "No, I am your father";
            Quote target = mock(Quote.class);
            when(target.getCorrectAnswer()).thenReturn(correctAnswer);

            // Symulujemy działanie MatchUtils i wywołujemy przekazaną lambdę (compareString)
            when(matchUtils.getMatch(eq(guess), eq(correctAnswer), any())).thenAnswer(invocation -> {
                BiFunction<String, String, MatchType> comparator = invocation.getArgument(2);
                MatchType matchType = comparator.apply(guess, correctAnswer);
                return new CardField<>(guess, matchType);
            });

            // ACT
            QuotesStageOneAttempt result = quoteMatchingService.guess(guess, target);

            // ASSERT
            assertNotNull(result);
            assertEquals(MatchType.NOT_MATCH, result.guess().match());
            assertEquals(guess, result.guess().value());
            verify(matchUtils).getMatch(eq(guess), eq(correctAnswer), any());
        }
    }
}