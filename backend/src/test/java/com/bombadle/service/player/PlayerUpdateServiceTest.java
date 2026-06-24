package com.bombadle.service.player;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.stats.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerUpdateServiceTest {

    @Mock
    private PlayerRepository repo;

    @Mock
    private CacheService cacheService;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private PlayerUpdateService playerUpdateService;

    @Nested
    class UpdatePlayerTests {

        private Player defaultPlayer;
        private final long defaultPlayerId = 1L;

        @BeforeEach
        void setUp() {
            defaultPlayer = Player.builder()
                    .id(defaultPlayerId)
                    .login("oldlogin")
                    .displayName("oldlogin")
                    .email("test@test.com")
                    .avatarImage(AvatarImage.AVATAR_DEFAULT)
                    .role(Role.ROLE_USER)
                    .authProvider(PlayerAuthProvider.LOCAL)
                    .createdAt(Instant.now())
                    .lastActiveAt(Instant.now())
                    .build();
        }

        @Test
        void updatePlayer_userNotFound_throwsUsernameNotFoundException() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("newLogin", "AVATAR_DEFAULT");
            when(repo.findById(defaultPlayerId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> playerUpdateService.updatePlayer(request, defaultPlayerId));
        }

        @Test
        void updatePlayer_loginTakenByAnotherUser_throwsRegistrationConflictException() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("takenLogin", "AVATAR_DEFAULT");

            when(repo.findById(defaultPlayerId)).thenReturn(Optional.of(defaultPlayer));
            when(repo.existsByLogin("takenlogin")).thenReturn(true);

            // Act & Assert
            assertThrows(RegistrationConflictException.class, () -> playerUpdateService.updatePlayer(request, defaultPlayerId));
        }

        @Test
        void updatePlayer_loginTooShort_throwsIllegalArgumentException() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("ab", "AVATAR_DEFAULT");

            when(repo.findById(defaultPlayerId)).thenReturn(Optional.of(defaultPlayer));
            when(repo.existsByLogin("ab")).thenReturn(false);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> playerUpdateService.updatePlayer(request, defaultPlayerId));
        }

        @Test
        void updatePlayer_validLoginChange_updatesSavesAndClearsCache() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("NewLogin", "AVATAR_DEFAULT");

            when(repo.findById(defaultPlayerId)).thenReturn(Optional.of(defaultPlayer));
            when(repo.existsByLogin("newlogin")).thenReturn(false);
            when(leaderboardService.getTop3Leaderboard(any(GameMode.class))).thenReturn(List.of());
            when(repo.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            playerUpdateService.updatePlayer(request, defaultPlayerId);

            // Assert
            assertEquals("newlogin", defaultPlayer.getLogin());
            assertEquals("NewLogin", defaultPlayer.getDisplayName());
            verify(repo).save(defaultPlayer);
            verify(cacheService).clear("paged-leaderboard");
            verify(cacheService).clear("top-3-leaderboard");
        }

        @Test
        void updatePlayer_invalidAvatar_throwsIllegalArgumentException() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("oldlogin", "INVALID_AVATAR");
            when(repo.findById(defaultPlayerId)).thenReturn(Optional.of(defaultPlayer));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> playerUpdateService.updatePlayer(request, defaultPlayerId));
        }

        @Test
        void updatePlayer_validAvatarChange_updatesSavesAndClearsCache() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("oldlogin", "AVATAR_BOMBA");

            when(repo.findById(defaultPlayerId)).thenReturn(Optional.of(defaultPlayer));
            when(leaderboardService.getTop3Leaderboard(any(GameMode.class))).thenReturn(List.of());
            when(repo.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            playerUpdateService.updatePlayer(request, defaultPlayerId);

            // Assert
            assertEquals(AvatarImage.AVATAR_BOMBA, defaultPlayer.getAvatarImage());
            verify(repo).save(defaultPlayer);
            verify(cacheService).clear("paged-leaderboard");
            verify(cacheService).clear("top-3-leaderboard");
        }

        @Test
        void updatePlayer_noProfileChangesButInTop3_clearsOnlyTop3Cache() {
            // Arrange
            PlayerUpdateRequest request = new PlayerUpdateRequest("oldlogin", "AVATAR_DEFAULT");

            LeaderboardEntryDto top3Entry = mock(LeaderboardEntryDto.class);
            when(top3Entry.playerId()).thenReturn(defaultPlayerId);

            when(repo.findById(defaultPlayerId)).thenReturn(Optional.of(defaultPlayer));
            when(leaderboardService.getTop3Leaderboard(any(GameMode.class))).thenReturn(List.of(top3Entry));
            when(repo.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            playerUpdateService.updatePlayer(request, defaultPlayerId);

            // Assert
            verify(repo).save(defaultPlayer);
            verify(cacheService, never()).clear("paged-leaderboard");
            verify(cacheService).clear("top-3-leaderboard");
        }
    }
}