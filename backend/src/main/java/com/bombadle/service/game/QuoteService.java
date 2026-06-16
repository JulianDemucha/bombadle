package com.bombadle.service.game;

import com.bombadle.entity.Quote;
import com.bombadle.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuoteService {
    private final QuoteRepository quoteRepository;

    public Quote findRandomQuote() {
        return quoteRepository.findRandomQuote();
    }
}
