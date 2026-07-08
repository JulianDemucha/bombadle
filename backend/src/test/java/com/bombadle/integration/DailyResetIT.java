package com.bombadle.integration;

import com.bombadle.entity.*;
import com.bombadle.enums.*;
import com.bombadle.repository.*;
import com.bombadle.service.game.CurrentCardStateService;
import com.bombadle.service.scheduling.DailyResetService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyResetIT extends BaseIT {

    @Autowired
    private DailyResetService dailyResetService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private GuessListRepository guessListRepository;

    @Autowired
    private AnonymousSessionRepository anonymousSessionRepository;

    @Autowired
    private AnonymousGuessListRepository anonymousGuessListRepository;

    @Autowired
    private AdminPendingChangeRepository adminPendingChangeRepository;

    @Autowired
    private CurrentCardStateService currentCardStateService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DeletedAccountRepository deletedAccountRepository;

    @Autowired
    private DeletedAccountStatisticRepository deletedAccountStatisticRepository;

    private Player activePlayer;
    private Player deletedPlayer;

    @BeforeEach
    void setUp() {
        Score activePlayerScore = scoreRepository.save(Score.builder()
                .numberOfTries(5)
                .gameMode(GameMode.CLASSIC)
                .scoreTimestamp(Instant.now())
                .build());
        scoreRepository.flush();

        activePlayer = Player.builder()
                .email("aktywny@gwiezdnaflota.com")
                .login("aktywny")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .totalSuccessfulGuesses(10)
                .avatarImage(AvatarImage.AVATAR_BOMBA)
                .todayScores(new HashMap<>(Map.of(GameMode.CLASSIC, activePlayerScore)))
                .authProvider(PlayerAuthProvider.LOCAL)
                .accountLocked(false)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .emailVerified(true)
                .build();
        activePlayer = playerRepository.save(activePlayer);
        activePlayer.addTodayScore(GameMode.CLASSIC, activePlayerScore);
        playerRepository.save(activePlayer);

        deletedPlayer = playerRepository.save(Player.builder()
                .email("usuniety@juzniesigma.com")
                .login("usuniety")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .totalSuccessfulGuesses(0)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .accountLocked(false)
                .createdAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .lastActiveAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .markedForDeletionAt(Instant.now().minus(35, ChronoUnit.DAYS))
                .build());

        guessListRepository.save(GuessList.builder()
                .player(activePlayer)
                .gameMode(GameMode.CLASSIC)
                .guesses(List.of())
                .build());

        AnonymousSession anonSession = AnonymousSession.builder()
                .lastActiveAt(Instant.now())
                .build();
        anonSession.markModeAsCompleted(GameMode.CLASSIC);
        anonymousSessionRepository.save(anonSession);

        adminPendingChangeRepository.save(AdminPendingChange.builder()
                .actionType("UPDATE_CARD")
                .actionKey("1")
                .createdAt(Instant.now())
                .payload("{}")
                .build());

        CharacterCard card = CharacterCard.builder()
                .name("Kapitan Bomba")
                .race(Race.Czlowiek)
                .alive(true)
                .firstAppearanceEpisode(1)
                .affiliations(Set.of(Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.BIALY))
                .gender(Gender.MALE)
                .build();
        entityManager.persist(card);

        // Two extra cards so the daily reset's CLASSIC/IMAGES picks have somewhere to land once
        // QUOTES_STAGE_2's card (the one above, via the quote below) is excluded.
        CharacterCard card2 = CharacterCard.builder()
                .name("Chorąży Torpeda")
                .race(Race.Czlowiek)
                .alive(true)
                .firstAppearanceEpisode(1)
                .affiliations(Set.of(Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.BIALY))
                .gender(Gender.MALE)
                .build();
        entityManager.persist(card2);

        CharacterCard card3 = CharacterCard.builder()
                .name("Porucznik Glus")
                .race(Race.Czlowiek)
                .alive(true)
                .firstAppearanceEpisode(1)
                .affiliations(Set.of(Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.BIALY))
                .gender(Gender.MALE)
                .build();
        entityManager.persist(card3);

        Quote quote = Quote.builder()
                .characterCard(card)
                .quoteBeginning("Nazywam się...")
                .options(List.of("Kapitan Bomba", "Chorąży Torpeda"))
                .correctAnswer("Kapitan Bomba")
                .appearanceEpisode(1)
                .target(QuoteTarget.SPEAKER)
                .build();
        entityManager.persist(quote);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void dailyReset_clearsTemporaryDataAndResetsPlayerState() {
        // Act
        dailyResetService.executeDailyReset();

        // evaluateDailyStreaks loads players into the persistence context, while the daily reset
        // clears completed_modes_today via a bulk native update that bypasses it. Clear the context
        // so the assertions below read the fresh database state instead of the stale L1 cache.
        entityManager.flush();
        entityManager.clear();

        // Assert
        Player resetActivePlayer = playerRepository.findById(activePlayer.getId()).orElseThrow();
        assertFalse(resetActivePlayer.hasGuessedToday(GameMode.CLASSIC));

        boolean deletedPlayerExists = playerRepository.existsById(deletedPlayer.getId());
        assertFalse(deletedPlayerExists);

        assertEquals(0, scoreRepository.count());
        assertEquals(0, guessListRepository.count());
        assertEquals(0, anonymousSessionRepository.count());
        assertEquals(0, anonymousGuessListRepository.count());
        assertEquals(0, adminPendingChangeRepository.count());

        CurrentCardState currentState = currentCardStateService.getCurrentCardState();
        assertFalse(currentState.getCurrentCards().isEmpty());
        assertNotNull(currentState.getCurrentQuote());
    }

    @Test
    void dailyReset_picksThreeMutuallyDistinctCardsForClassicQuotesStage2AndImages() {
        // Act
        dailyResetService.executeDailyReset();

        entityManager.flush();
        entityManager.clear();

        // Assert
        Map<GameMode, CharacterCard> currentCards = currentCardStateService.getCurrentCardState().getCurrentCards();
        Long classicId = currentCards.get(GameMode.CLASSIC).getId();
        Long imagesId = currentCards.get(GameMode.IMAGES).getId();
        Long quotesStage2Id = currentCards.get(GameMode.QUOTES_STAGE_2).getId();

        assertEquals(Set.of(classicId, imagesId, quotesStage2Id).size(), 3,
                "CLASSIC, IMAGES and QUOTES_STAGE_2 must each get a different card");
    }

    @Test
    void dailyReset_purgesDeletedAccountSnapshotsOlderThan7DaysButKeepsRecentOnes() {
        // Arrange
        DeletedAccount expiredAccount = deletedAccountRepository.save(DeletedAccount.builder()
                .originalPlayerId(100L)
                .login("expired")
                .email("expired@mail.com")
                .role(Role.ROLE_USER)
                .createdAt(Instant.now().minus(100, ChronoUnit.DAYS))
                .totalSuccessfulGuesses(0)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .deletedAt(Instant.now().minus(8, ChronoUnit.DAYS))
                .build());
        deletedAccountStatisticRepository.save(DeletedAccountStatistic.builder()
                .deletedAccountId(expiredAccount.getId())
                .currentStreak(0).longestStreak(0).currentSuperstreak(0).longestSuperstreak(0)
                .totalSuccessfulGuesses(0)
                .totalTop3Finishes(0)
                .capturedAt(Instant.now().minus(8, ChronoUnit.DAYS))
                .build());

        DeletedAccount recentAccount = deletedAccountRepository.save(DeletedAccount.builder()
                .originalPlayerId(101L)
                .login("recent")
                .email("recent@mail.com")
                .role(Role.ROLE_USER)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .totalSuccessfulGuesses(0)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .deletedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());
        deletedAccountStatisticRepository.save(DeletedAccountStatistic.builder()
                .deletedAccountId(recentAccount.getId())
                .currentStreak(0).longestStreak(0).currentSuperstreak(0).longestSuperstreak(0)
                .totalSuccessfulGuesses(0)
                .totalTop3Finishes(0)
                .capturedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());

        entityManager.flush();
        entityManager.clear();

        // Act
        dailyResetService.executeDailyReset();

        entityManager.flush();
        entityManager.clear();

        // Assert
        assertFalse(deletedAccountRepository.existsById(expiredAccount.getId()));
        assertTrue(deletedAccountStatisticRepository.findByDeletedAccountId(expiredAccount.getId()).isEmpty());

        assertTrue(deletedAccountRepository.existsById(recentAccount.getId()));
        assertTrue(deletedAccountStatisticRepository.findByDeletedAccountId(recentAccount.getId()).isPresent());
    }
}