package com.bombadle.integration;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.TodaySolversDto;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.AnonymousSessionRepository;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.stats.TodaySolversService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaderboardIT extends BaseIT {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private AnonymousSessionRepository anonymousSessionRepository;

    @Autowired
    private TodaySolversService todaySolversService;

    private Player persistPlayerWithScore(String login, int currentStreak, int wins, GameMode gameMode) {
        Player player = Player.builder()
                .login(login)
                .displayName(login)
                .email(login + "@bombadle.com")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .avatarImage(AvatarImage.AVATAR_BOMBA)
                .authProvider(PlayerAuthProvider.LOCAL)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .totalSuccessfulGuesses(wins)
                .currentStreak(currentStreak)
                .completedModesToday(new HashSet<>(Set.of(gameMode)))
                .emailVerified(true)
                .build();
        player = playerRepository.save(player);

        scoreRepository.save(Score.builder()
                .player(player)
                .gameMode(gameMode)
                .numberOfTries(4)
                .scoreTimestamp(Instant.now())
                .build());
        scoreRepository.flush();

        return player;
    }

    private void persistAnonymousSession(GameMode... completedModes) {
        AnonymousSession session = AnonymousSession.createEmptySession();
        for (GameMode mode : completedModes) {
            session.markModeAsCompleted(mode);
        }
        anonymousSessionRepository.save(session);
        anonymousSessionRepository.flush();
    }

    @Test
    void findTop3_mapsCurrentStreakFromPlayer() {
        persistPlayerWithScore("top3streaker", 7, 15, GameMode.CLASSIC);

        List<LeaderboardEntryDto> result = scoreRepository.findTop3(GameMode.CLASSIC);

        assertEquals(1, result.size());
        LeaderboardEntryDto entry = result.get(0);
        assertEquals(7, entry.currentStreak());
        // wins must remain intact alongside the new field
        assertEquals(15, entry.wins());
    }

    @Test
    void findPagedLeaderboard_mapsCurrentStreakFromPlayer() {
        persistPlayerWithScore("pagedstreaker", 3, 8, GameMode.IMAGES);

        Page<LeaderboardEntryDto> result =
                scoreRepository.findPagedLeaderboard(GameMode.IMAGES, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        LeaderboardEntryDto entry = result.getContent().get(0);
        assertEquals(3, entry.currentStreak());
        assertEquals(8, entry.wins());
    }

    @Test
    void findLeaderboardRankedEntryByPlayerId_mapsCurrentStreakFromPlayer() {
        Player player = persistPlayerWithScore("rankedstreaker", 12, 20, GameMode.QUOTES_STAGE_2);

        Optional<LeaderboardEntryDto> result =
                scoreRepository.findLeaderboardRankedEntryByPlayerId(GameMode.QUOTES_STAGE_2, player.getId());

        assertTrue(result.isPresent());
        assertEquals(12, result.get().currentStreak());
        assertEquals(20, result.get().wins());
    }

    @Test
    void getTodaySolvers_countsLoggedInScoresAndAnonymousJsonbSessions() {
        // logged-in: two solved CLASSIC, one solved IMAGES (must not count for CLASSIC)
        persistPlayerWithScore("solver1", 1, 1, GameMode.CLASSIC);
        persistPlayerWithScore("solver2", 1, 1, GameMode.CLASSIC);
        persistPlayerWithScore("solver3", 1, 1, GameMode.IMAGES);

        // anonymous: three completed CLASSIC, one completed only IMAGES
        persistAnonymousSession(GameMode.CLASSIC);
        persistAnonymousSession(GameMode.CLASSIC);
        persistAnonymousSession(GameMode.CLASSIC);
        persistAnonymousSession(GameMode.IMAGES);

        TodaySolversDto result = todaySolversService.getTodaySolvers(GameMode.CLASSIC);

        assertEquals(2L, result.loggedIn());
        assertEquals(3L, result.anonymous());
    }

    @Test
    void getTodaySolvers_quotesStageTwoCountsOnlyFullQuotesSolvers() {
        // a stage-1-only session is not a full quotes solver and must not appear on the
        // QUOTES_STAGE_2 leaderboard count; a session that reached stage 2 must.
        persistAnonymousSession(GameMode.QUOTES_STAGE_1);
        persistAnonymousSession(GameMode.QUOTES_STAGE_1, GameMode.QUOTES_STAGE_2);

        TodaySolversDto result = todaySolversService.getTodaySolvers(GameMode.QUOTES_STAGE_2);

        assertEquals(0L, result.loggedIn());
        assertEquals(1L, result.anonymous());
    }
}
