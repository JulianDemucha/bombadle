package com.bombadle.service.game;

import com.bombadle.dto.QuotesStageOneAttempt;
import com.bombadle.entity.Quote;
import com.bombadle.enums.MatchType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuoteMatchingService {
    private final MatchUtils matchUtils;

    public QuotesStageOneAttempt guess(String guess, Quote target) {
        return new QuotesStageOneAttempt(
                matchUtils.getMatch(guess, target.getCorrectAnswer(), this::compareString)
        );
    }

    // todo move to MatchUtils alongside with the rest comparators from CardMatchingService
    private MatchType compareString(String guess, String target) {
        return target.equals(guess) ? MatchType.MATCH : MatchType.NOT_MATCH;
    }
}
