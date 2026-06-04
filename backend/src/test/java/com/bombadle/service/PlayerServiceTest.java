package com.bombadle.service;

import com.bombadle.dto.PlayerDto;
import com.bombadle.exception.UsernameAlreadyTakenException;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.stats.LeaderboardService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository repo;

    @Mock
    private PlayerDeletionService playerDeletionService;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private PlayerService playerService;

    private Player buildPlayer(long id, String email, String login) {
        return Player.builder()
                .id(id)
                .login(login.toLowerCase())
                .displayName(login)
                .email(email.toLowerCase())
                .passwordHash("test")
                .role(Role.ROLE_USER)
                .createdAt(Instant.parse("2025-11-10T14:22:27.123Z"))
                .lastActiveAt(Instant.parse("2025-11-10T14:22:27.123Z"))
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .totalSuccessfulGuesses(0)
                .hasGuessedToday(false)
                .authProvider(PlayerAuthProvider.LOCAL)
                .build();
    }

    @Nested
    class GetAuthenticatedPlayerTests {

        @Test
        void getAuthenticatedPlayer_userExists_returnsDto() {
            // Arrange
            long playerId = 1L;
            String email = "test@gmail.com";
            Player player = buildPlayer(playerId, email, "test");
            PlayerDto dto = PlayerDto.toDto(player);

            when(repo.findById(playerId)).thenReturn(Optional.of(player));

            // Act
            PlayerDto returnedDto = playerService.getAuthenticatedPlayer(playerId);

            // Assert
            assertEquals(dto, returnedDto);
            verify(repo).findById(playerId);
        }

        @Test
        void getAuthenticatedPlayer_userDoesNotExist_throwsUsernameNotFoundException() {
            // Arrange
            long playerId = 1L;
            when(repo.findById(playerId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> playerService.getAuthenticatedPlayer(playerId));
            verify(repo).findById(playerId);
        }
    }

    @Nested
    class UpdatePlayerTests {

        @Test
        void updatePlayer_DataIsValid_savesPlayerAndReturnsDto() {
            // Arrange
            long playerId = 1L;
            String email = "test@test.com";
            Player existingPlayer = buildPlayer(playerId, email, "test");

            PlayerUpdateRequest request = new PlayerUpdateRequest("TestTest", "AVATAR_DEFAULT");

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of());
            when(repo.existsByLogin("testtest")).thenReturn(false);

            // Act
            PlayerDto returnedDto = playerService.updatePlayer(request, playerId);

            // Assert
            PlayerDto expectedDto = PlayerDto.toDto(existingPlayer);
            assertEquals(expectedDto, returnedDto);
            assertEquals("testtest", existingPlayer.getLogin());
            assertEquals("TestTest", existingPlayer.getDisplayName());
            assertEquals(AvatarImage.AVATAR_DEFAULT, existingPlayer.getAvatarImage());

            verify(repo).findById(playerId);
            verify(repo).save(existingPlayer);
        }

        @Test
        void updatePlayer_userDoesNotExist_throwsUsernameNotFoundException() {
            // Arrange
            long playerId = 1L;
            PlayerUpdateRequest request = new PlayerUpdateRequest("testtest", "AVATAR_DEFAULT");
            when(repo.findById(playerId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> playerService.updatePlayer(request, playerId));
            verify(repo).findById(playerId);
        }

        @Test
        void updatePlayer_loginLengthIsInvalid_throwsIllegalArgumentException() {
            // Arrange
            long playerId = 1L;
            String email = "test@test.com";
            Player existingPlayer = buildPlayer(playerId, email, "test");

            PlayerUpdateRequest requestTooShort = new PlayerUpdateRequest("te", "AVATAR_DEFAULT");
            PlayerUpdateRequest requestTooLong = new PlayerUpdateRequest("testtesttesttestt", "AVATAR_DEFAULT");

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(requestTooShort, playerId));
            assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(requestTooLong, playerId));

            verify(repo, times(2)).findById(playerId);
        }

        @Test
        void updatePlayer_loginIsAlreadyTaken_throwsUsernameAlreadyTakenException() {
            // Arrange
            long playerId = 1L;
            String email = "test@test.com";
            Player existingPlayer = buildPlayer(playerId, email, "test");

            PlayerUpdateRequest request = new PlayerUpdateRequest("Test1", "AVATAR_DEFAULT");

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of());
            when(repo.existsByLogin("test1")).thenReturn(true);

            // Act & Assert
            assertThrows(UsernameAlreadyTakenException.class, () -> playerService.updatePlayer(request, playerId));
            verify(repo).findById(playerId);
        }

        @Test
        void updatePlayer_imageIsInvalid_throwsIllegalArgumentException() {
            // Arrange
            long playerId = 1L;
            String email = "test@test.com";
            Player existingPlayer = buildPlayer(playerId, email, "test");

            PlayerUpdateRequest request = new PlayerUpdateRequest("test", "pararara ramapampampam");

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(request, playerId));
            verify(repo).findById(playerId);
        }
    }
}