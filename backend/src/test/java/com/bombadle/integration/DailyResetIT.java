package com.bombadle.integration;

import com.bombadle.entity.*;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.Gender;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.*;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyResetIT {

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
    private EntityManager entityManager;

    private Player activePlayer;
    private Player deletedPlayer;
    @BeforeEach
    void setUp() {
        Score activePlayerScore = scoreRepository.save(Score.builder()
                .numberOfTries(5)
                .scoreTimestamp(Instant.now())
                .build());
        scoreRepository.flush();

        activePlayer = playerRepository.save(Player.builder()
                .email("aktywny@gwiezdnaflota.com")
                .login("aktywny")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .hasGuessedToday(true)
                .todayScore(activePlayerScore)
                .totalSuccessfulGuesses(10)
                .avatarImage(AvatarImage.AVATAR_BOMBA)
                .authProvider(PlayerAuthProvider.LOCAL)
                .accountLocked(false)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .build());

        deletedPlayer = playerRepository.save(Player.builder()
                .email("usuniety@kosmici.com")
                .login("usuniety")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .hasGuessedToday(false)
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
                .guesses(List.of())
                .build());

        anonymousSessionRepository.save(AnonymousSession.builder()
                .guessList(AnonymousGuessList.builder().guesses(List.of()).build())
                .hasGuessedToday(true)
                .lastActiveAt(Instant.now())
                .build());

        adminPendingChangeRepository.save(AdminPendingChange.builder()
                .actionType("UPDATE_CARD")
                .actionKey("1")
                .createdAt(Instant.now())
                .payload("{}")
                .build());

        entityManager.persist(CharacterCard.builder()
                .name("Bomba")
                .gender(Gender.MALE)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void dailyReset_clearsTemporaryDataAndResetsPlayerState() {
        // Act
        dailyResetService.pickNewCharacterCardAndResetScores();

        // Assert - Players cleared of flags and scores
        Player resetActivePlayer = playerRepository.findById(activePlayer.getId()).orElseThrow();
        assertFalse(resetActivePlayer.getHasGuessedToday());
        assertNull(resetActivePlayer.getTodayScore());

        // Assert - Accounts marked for deletion are removed
        boolean deletedPlayerExists = playerRepository.existsById(deletedPlayer.getId());
        assertFalse(deletedPlayerExists);

        // Assert - Temporary tables truncated/cleared to zero
        assertEquals(0, scoreRepository.count());
        assertEquals(0, guessListRepository.count());
        assertEquals(0, anonymousSessionRepository.count());
        assertEquals(0, anonymousGuessListRepository.count());
        assertEquals(0, adminPendingChangeRepository.count());
    }

}