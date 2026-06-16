package com.bombadle.service.game;

import com.bombadle.entity.Quote;
import com.bombadle.repository.QuoteRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    @InjectMocks
    private QuoteService quoteService;

    @Nested
    class FindRandomQuoteTests {

        @Test
        void findRandomQuote_quoteExists_returnsQuote() {
            // ARRANGE
            Quote expectedQuote = mock(Quote.class);
            when(quoteRepository.findRandomQuote()).thenReturn(expectedQuote);

            // ACT
            Quote result = quoteService.findRandomQuote();

            // ASSERT
            assertEquals(expectedQuote, result);
            verify(quoteRepository).findRandomQuote();
        }

        @Test
        void findRandomQuote_noQuotesInDatabase_returnsNull() {
            // ARRANGE
            when(quoteRepository.findRandomQuote()).thenReturn(null);

            // ACT
            Quote result = quoteService.findRandomQuote();

            // ASSERT
            assertNull(result);
            verify(quoteRepository).findRandomQuote();
        }
    }
}