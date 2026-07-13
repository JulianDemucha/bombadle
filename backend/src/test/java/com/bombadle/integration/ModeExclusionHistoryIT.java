package com.bombadle.integration;

import com.bombadle.entity.*;
import com.bombadle.enums.*;
import com.bombadle.repository.ModeExclusionHistoryRepository;
import com.bombadle.service.game.CurrentCardStateService;
import com.bombadle.service.scheduling.DailyResetService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModeExclusionHistoryIT extends BaseIT {

    @Autowired
    private DailyResetService dailyResetService;

    @Autowired
    private CurrentCardStateService currentCardStateService;

    @Autowired
    private ModeExclusionHistoryRepository modeExclusionHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    private CharacterCard card1;
    private CharacterCard card2;
    private CharacterCard card3;
    private Quote quote1;
    private Quote quote2;

    @BeforeEach
    void setUp() {
        card1 = persistCard("Kapitan Bomba");
        card2 = persistCard("Chorąży Torpeda");
        card3 = persistCard("Porucznik Glus");

        // Both quotes are tied to card1, so QUOTES_STAGE_2's card stays fixed across every reset
        // below and only card2/card3 are ever in play for CLASSIC/IMAGES.
        quote1 = persistQuote(card1, "Nazywam się...", "Kapitan Bomba");
        quote2 = persistQuote(card1, "Jestem...", "Kapitan Bomba");

        entityManager.flush();
        entityManager.clear();
    }

    private CharacterCard persistCard(String name) {
        CharacterCard card = CharacterCard.builder()
                .name(name)
                .race(Race.Czlowiek)
                .alive(true)
                .firstAppearanceEpisode(1)
                .affiliations(Set.of(Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.BIALY))
                .gender(Gender.MALE)
                .build();
        entityManager.persist(card);
        return card;
    }

    private Quote persistQuote(CharacterCard card, String beginning, String correctAnswer) {
        Quote quote = Quote.builder()
                .characterCard(card)
                .quoteBeginning(beginning)
                .options(List.of(correctAnswer, "Inny"))
                .correctAnswer(correctAnswer)
                .appearanceEpisode(1)
                .target(QuoteTarget.SPEAKER)
                .build();
        entityManager.persist(quote);
        return quote;
    }

    @Test
    void repeatedDailyResets_exhaustThenResetCardAndQuotePoolsWithoutImmediateRepeats() {
        // Day 1: pools are fully open. With 3 cards total (one fixed by the quote), CLASSIC and
        // IMAGES deterministically split the two remaining cards between them.
        dailyResetService.executeDailyReset();
        entityManager.flush();
        entityManager.clear();

        Map<GameMode, CharacterCard> day1Cards = currentCardStateService.getCurrentCardState().getCurrentCards();
        Long day1Classic = day1Cards.get(GameMode.CLASSIC).getId();
        Long day1Images = day1Cards.get(GameMode.IMAGES).getId();
        Long day1Quote = currentCardStateService.getCurrentCardState().getCurrentQuote().getId();

        assertEquals(Set.of(card2.getId(), card3.getId()), Set.of(day1Classic, day1Images));
        assertTrue(Set.of(quote1.getId(), quote2.getId()).contains(day1Quote));

        // Day 2: CLASSIC/IMAGES history now excludes day 1's picks, forcing each onto the other
        // remaining card; the quote history forces the other quote too.
        dailyResetService.executeDailyReset();
        entityManager.flush();
        entityManager.clear();

        Map<GameMode, CharacterCard> day2Cards = currentCardStateService.getCurrentCardState().getCurrentCards();
        Long day2Classic = day2Cards.get(GameMode.CLASSIC).getId();
        Long day2Images = day2Cards.get(GameMode.IMAGES).getId();
        Long day2Quote = currentCardStateService.getCurrentCardState().getCurrentQuote().getId();

        assertNotEquals(day1Classic, day2Classic);
        assertNotEquals(day1Images, day2Images);
        assertNotEquals(day1Quote, day2Quote);
        assertEquals(Set.of(quote1.getId(), quote2.getId()), Set.of(day1Quote, day2Quote));

        // Day 3: both pools (3 cards, 2 quotes) are now fully excluded by history, forcing a
        // reset-to-most-recent-pick for CLASSIC, IMAGES and the quote pool, then a fresh draw.
        dailyResetService.executeDailyReset();
        entityManager.flush();
        entityManager.clear();

        Map<GameMode, CharacterCard> day3Cards = currentCardStateService.getCurrentCardState().getCurrentCards();
        Long day3Classic = day3Cards.get(GameMode.CLASSIC).getId();
        Long day3Images = day3Cards.get(GameMode.IMAGES).getId();
        Long day3Quote = currentCardStateService.getCurrentCardState().getCurrentQuote().getId();

        // No immediate repeat of day 2's picks...
        assertNotEquals(day2Classic, day3Classic);
        assertNotEquals(day2Images, day3Images);
        assertNotEquals(day2Quote, day3Quote);
        // ...but with only 3 cards / 2 quotes total, the cycle restarting lands day 3 back on day 1's picks.
        assertEquals(day1Classic, day3Classic);
        assertEquals(day1Images, day3Images);
        assertEquals(day1Quote, day3Quote);

        // History rows reflect the post-reset state: the most-recent pick plus the freshly drawn one.
        ModeExclusionHistory classicHistory = modeExclusionHistoryRepository.findByGameMode(GameMode.CLASSIC).orElseThrow();
        ModeExclusionHistory imagesHistory = modeExclusionHistoryRepository.findByGameMode(GameMode.IMAGES).orElseThrow();
        ModeExclusionHistory quoteHistory = modeExclusionHistoryRepository.findByGameMode(GameMode.QUOTES_STAGE_1).orElseThrow();

        assertEquals(Set.of(day2Classic, day3Classic), classicHistory.getExcludedIds());
        assertEquals(Set.of(day2Images, day3Images), imagesHistory.getExcludedIds());
        assertEquals(Set.of(day2Quote, day3Quote), quoteHistory.getExcludedIds());

        assertTrue(modeExclusionHistoryRepository.findByGameMode(GameMode.QUOTES_STAGE_2).isEmpty(),
                "QUOTES_STAGE_2 must never get its own exclusion history row");
    }
}
