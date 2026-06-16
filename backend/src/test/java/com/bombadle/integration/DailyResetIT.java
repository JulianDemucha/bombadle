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

        entityManager.persist(CharacterCard.builder()
                .name("Kapitan Bomba")
                .race(Race.Czlowiek)
                .alive(true)
                .firstAppearanceEpisode(1)
                .affiliations(Set.of(Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.BIALY))
                .gender(Gender.MALE)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void dailyReset_clearsTemporaryDataAndResetsPlayerState() {
        // Act
        dailyResetService.pickNewCharacterCardAndResetScores();

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

        assertFalse(currentCardStateService.getCurrentCardState().getCurrentCards().isEmpty());
    }
}