package com.bombadle.integration;

import com.bombadle.dto.CardField;
import com.bombadle.dto.NameOnlyGuessAttempt;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.MatchType;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.game.GuessRegistrationService;
import com.bombadle.service.game.ScoreRegistrationService;
import com.bombadle.service.stats.LeaderboardService;
import com.bombadle.service.stats.PlayerStatisticsService;
import com.bombadle.service.stats.TodaySolversService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CacheEvictionIT extends BaseIT {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GuessListService guessListService;

    @Autowired
    private GuessRegistrationService guessRegistrationService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private ScoreRegistrationService scoreRegistrationService;

    @Autowired
    private TodaySolversService todaySolversService;

    @Autowired
    private PlayerStatisticsService playerStatisticsService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
    }

    private Player persistPlayer(String login) {
        return playerRepository.save(Player.builder()
                .login(login)
                .displayName(login)
                .email(login + "@bombadle.com")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .avatarImage(AvatarImage.AVATAR_BOMBA)
                .authProvider(PlayerAuthProvider.LOCAL)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .emailVerified(true)
                .build());
    }

    private NameOnlyGuessAttempt incorrectGuess(String name) {
        return new NameOnlyGuessAttempt(new CardField<>(name, MatchType.NOT_MATCH));
    }

    @Test
    void guessList_secondGuessOnAlreadyPersistedList_isPersistedAndVisibleAfterEviction() {
        Player player = persistPlayer("guesser1");

        guessRegistrationService.registerGuess(player, incorrectGuess("Wrong1"), GameMode.IMAGES);
        // Warms the "guess-list" cache for this player+mode with a 1-entry list.
        assertEquals(1, guessListService.getByPlayerId(player.getId(), GameMode.IMAGES).guessList().size());

        guessRegistrationService.registerGuess(player, incorrectGuess("Wrong2"), GameMode.IMAGES);
        // Fails before the fix in two independent ways: a missing/renamed cache eviction would
        // return the stale 1-entry cached list, while the jsonb in-place-mutation bug would mean
        // the second guess was never written to the DB at all, so even a correct cache miss would
        // still read back only 1 entry.
        assertEquals(2, guessListService.getByPlayerId(player.getId(), GameMode.IMAGES).guessList().size());
    }

    @Test
    void fullLeaderboard_secondPlayerScores_evictsStaleCachedPage() {
        Player player1 = persistPlayer("leader1");
        Player player2 = persistPlayer("leader2");

        scoreRegistrationService.registerPlayerWin(player1.getId(), 3, GameMode.CLASSIC);
        // Warms the "full-leaderboard" cache for CLASSIC-page0 with 1 entry.
        assertEquals(1, leaderboardService.getPagedLeaderboard(GameMode.CLASSIC, 0).getTotalElements());

        scoreRegistrationService.registerPlayerWin(player2.getId(), 5, GameMode.CLASSIC);
        // Fails before the fix: ScoreRegistrationService used to clear a cache named
        // "paged-leaderboard", which doesn't exist, so the real "full-leaderboard" cache was never
        // evicted and this would still read back 1.
        assertEquals(2, leaderboardService.getPagedLeaderboard(GameMode.CLASSIC, 0).getTotalElements());
    }

    @Test
    void todaySolvers_secondPlayerScores_evictsStaleCachedCount() {
        Player player1 = persistPlayer("solverA");
        Player player2 = persistPlayer("solverB");

        scoreRegistrationService.registerPlayerWin(player1.getId(), 3, GameMode.IMAGES);
        // Warms the "today-solvers" cache for IMAGES with loggedIn=1.
        assertEquals(1, todaySolversService.getTodaySolvers(GameMode.IMAGES).loggedIn());

        scoreRegistrationService.registerPlayerWin(player2.getId(), 5, GameMode.IMAGES);
        assertEquals(2, todaySolversService.getTodaySolvers(GameMode.IMAGES).loggedIn());
    }

    @Test
    void playerBasicStatistics_secondWinInDifferentMode_evictsStaleCachedTotalGuesses() {
        Player player = persistPlayer("statser1");

        scoreRegistrationService.registerPlayerWin(player.getId(), 3, GameMode.CLASSIC);
        // Warms the "player-basic-statistics" cache for this player with totalGuesses=1.
        assertEquals(1, playerStatisticsService.getBasicStatistics(player.getId()).totalGuesses());

        scoreRegistrationService.registerPlayerWin(player.getId(), 4, GameMode.IMAGES);
        assertEquals(2, playerStatisticsService.getBasicStatistics(player.getId()).totalGuesses());
    }
}
