package com.bombadle.service;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {

    @Mock
    private ScoreRepository scoreRepository;

    @InjectMocks
    private ScoreService scoreService;

    private Player getExamplePlayer() {
        return Player.builder()
                .id(1L)
                .login("test")
                .passwordHash("test")
                .email("test@test.test")
                .authProvider(PlayerAuthProvider.LOCAL)
                .role(Role.ROLE_USER)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .hasGuessedToday(true)
                .totalSuccessfulGuesses(1)
                .build();
    }

    @Nested
    class SaveScoreTests {

        @Test
        void saveScore_addsScoreSuccessfully() {
            // Arrange
            Player player = getExamplePlayer();
            Score score = Score.builder()
                    .player(player)
                    .scoreTimestamp(Instant.now())
                    .numberOfTries(5)
                    .build();
            score.setId(1L);
            player.setTodayScore(score);

            when(scoreRepository.save(score)).thenReturn(score);

            // Act
            Score savedScore = scoreService.saveScore(score);

            // Assert
            assertNotNull(savedScore);
            assertEquals(player.getEmail(), savedScore.getPlayer().getEmail());
            assertEquals(score.getId(), savedScore.getId());
            verify(scoreRepository, times(1)).save(score);
        }

        @Test
        void saveScore_scoreIsNull_throwsIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> scoreService.saveScore(null));
        }
    }

    @Nested
    class FindScoreTests {

        @Test
        void getScoreByPlayerId_returnsScoreSuccessfully() {
            // Arrange
            Player player = getExamplePlayer();
            Score score = Score.builder()
                    .player(player)
                    .scoreTimestamp(Instant.now())
                    .numberOfTries(1)
                    .build();

            when(scoreRepository.findByPlayerId(player.getId())).thenReturn(Optional.of(score));

            // Act
            Optional<Score> result = scoreService.findScoreByPlayerId(player.getId());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(player.getId(), result.get().getPlayer().getId());
        }
    }
}