package com.bombadle.service.game;

import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.ModeExclusionHistory;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.ModeExclusionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ModeExclusionHistoryService {
    private final ModeExclusionHistoryRepository repo;
    private final CharacterCardService characterCardService;
    private final QuoteService quoteService;

    @Transactional
    public CharacterCard pickCardForMode(GameMode mode, List<Long> sameDayExcludedIds, CharacterCard mostRecentCard) {
        ModeExclusionHistory history = getOrCreate(mode);

        CharacterCard picked = drawCard(union(history.getExcludedIds(), sameDayExcludedIds));
        if (picked == null) {
            history.resetExcludedIdsTo(mostRecentCard != null ? mostRecentCard.getId() : null);
            picked = drawCard(union(history.getExcludedIds(), sameDayExcludedIds));
        }
        if (picked == null) {
            throw new IllegalStateException("No character cards in the database for mode: " + mode);
        }

        history.addExcludedId(picked.getId());
        repo.save(history);
        return picked;
    }

    @Transactional
    public Quote pickQuote(Quote mostRecentQuote) {
        ModeExclusionHistory history = getOrCreate(GameMode.QUOTES_STAGE_1);

        Quote picked = drawQuote(history.getExcludedIds());
        if (picked == null) {
            history.resetExcludedIdsTo(mostRecentQuote != null ? mostRecentQuote.getId() : null);
            picked = drawQuote(history.getExcludedIds());
        }
        if (picked == null) {
            throw new IllegalStateException("No quotes in the database");
        }

        history.addExcludedId(picked.getId());
        repo.save(history);
        return picked;
    }

    private CharacterCard drawCard(List<Long> excludedIds) {
        return excludedIds.isEmpty()
                ? characterCardService.findRandomCard()
                : characterCardService.findRandomCardExcluding(excludedIds);
    }

    private Quote drawQuote(Set<Long> excludedIds) {
        return excludedIds.isEmpty()
                ? quoteService.findRandomQuote()
                : quoteService.findRandomQuoteExcluding(new ArrayList<>(excludedIds));
    }

    private ModeExclusionHistory getOrCreate(GameMode mode) {
        return repo.findByGameMode(mode)
                .orElseGet(() -> ModeExclusionHistory.builder().gameMode(mode).build());
    }

    private List<Long> union(Set<Long> historyIds, List<Long> sameDayIds) {
        Set<Long> combined = new HashSet<>(historyIds);
        combined.addAll(sameDayIds);
        return new ArrayList<>(combined);
    }
}
