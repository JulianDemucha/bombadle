package com.bombadle.service;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {
    @Mock
    ScoreRepository scoreRepository;
    @InjectMocks
    ScoreService scoreService;

    Player getExamplePlayer() {
        Player player = new Player();
        player.setId((long) 1);
        player.setLogin("test");
        player.setPasswordHash("test");
        player.setEmail("test@test.test");
        player.setAuthProvider(PlayerAuthProvider.LOCAL);
        player.setRole(Role.ROLE_USER);
        player.setAvatarImage(AvatarImage.AVATAR_DEFAULT);
        player.setCreatedAt(Instant.now());
        player.setLastLoginAt(Instant.now());
        player.setHasGuessedToday(true);
        player.setTotalSuccessfulGuesses(1);
        return player;
    }

    @Test
    void saveScoreShouldAddScoreSuccessfully() {
        Player player = getExamplePlayer();
        Score score = new Score();
        score.setScoreTimestamp(Instant.now());
        score.setId((long) 1);
        score.setNumberOfTries(5);
        score.setPlayer(player);
        player.setTodayScore(score);

        when(scoreRepository.save(score)).thenReturn(score);
        Score savedScore = scoreService.saveScore(score);

        Assertions.assertNotNull(savedScore);
        Assertions.assertEquals(player.getEmail(), savedScore.getPlayer().getEmail());
        Assertions.assertEquals(score.getId(), savedScore.getId());
        verify(scoreRepository, times(1)).save(score);
    }

    @Test
    void saveScoreShouldThrowExceptionWhenScoreIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> scoreService.saveScore(null));
    }

    @Test
    void getScoreByPlayerIdShouldReturnScoreSuccessfully() {
        Player player = getExamplePlayer();
        Score score = new Score();
        score.setPlayer(player);

        when(scoreRepository.findByPlayerId(player.getId())).thenReturn(Optional.of(score));

        Optional<Score> result = scoreService.findScoreByPlayerId(player.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(player.getId(), result.get().getPlayer().getId());
    }

}
