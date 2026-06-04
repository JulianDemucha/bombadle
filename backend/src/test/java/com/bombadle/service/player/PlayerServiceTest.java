package com.bombadle.service.player;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.PlayerDto;
import com.bombadle.exception.UsernameAlreadyTakenException;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.stats.LeaderboardService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    class FindByEmailTests {

        @Test
        void findByEmail_emailIsNull_returnsEmptyOptional() {
            // Act
            Optional<Player> result = playerService.findByEmail(null);

            // Assert
            assertTrue(result.isEmpty());
            verifyNoInteractions(repo);
        }

        @Test
        void findByEmail_emailIsValid_callsRepoWithLowercase() {
            // Arrange
            String email = "Test@Mail.Com";
            Player player = mock(Player.class);
            when(repo.findByEmail("test@mail.com")).thenReturn(Optional.of(player));

            // Act
            Optional<Player> result = playerService.findByEmail(email);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(player, result.get());
        }
    }

    @Nested
    class FindByLoginNormalizedTests {

        @Test
        void findByLoginNormalized_loginIsNull_returnsEmptyOptional() {
            // Act
            Optional<Player> result = playerService.findByLoginNormalized(null);

            // Assert
            assertTrue(result.isEmpty());
            verifyNoInteractions(repo);
        }

        @Test
        void findByLoginNormalized_loginIsValid_callsRepoWithLowercase() {
            // Arrange
            String login = "TestLogin";
            Player player = mock(Player.class);
            when(repo.findByLogin("testlogin")).thenReturn(Optional.of(player));

            // Act
            Optional<Player> result = playerService.findByLoginNormalized(login);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(player, result.get());
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_callsRepository() {
            // Arrange
            long id = 1L;
            Player player = mock(Player.class);
            when(repo.findById(id)).thenReturn(Optional.of(player));

            // Act
            Optional<Player> result = playerService.findById(id);

            // Assert
            assertTrue(result.isPresent());
            verify(repo).findById(id);
        }
    }

    @Nested
    class GetAllPlayersTests {

        @Test
        void getAllPlayers_returnsList() {
            // Arrange
            List<Player> expectedList = List.of(mock(Player.class));
            when(repo.findAllByOrderByIdAsc()).thenReturn(expectedList);

            // Act
            List<Player> result = playerService.getAllPlayers();

            // Assert
            assertEquals(expectedList, result);
        }

        @Test
        void getAllPlayersWithPageable_returnsPage() {
            // Arrange
            Pageable pageable = mock(Pageable.class);
            Page<Player> expectedPage = mock(Page.class);
            when(repo.findAllByOrderByIdAsc(pageable)).thenReturn(expectedPage);

            // Act
            Page<Player> result = playerService.getAllPlayers(pageable);

            // Assert
            assertEquals(expectedPage, result);
        }
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
    class RegisterScoreTests {

        @Test
        void registerScore_updatesPlayerAndSaves() {
            // Arrange
            Player player = buildPlayer(1L, "test@test.com", "test");
            Score score = mock(Score.class);
            int initialGuesses = player.getTotalSuccessfulGuesses();

            // Act
            playerService.registerScore(player, score);

            // Assert
            assertTrue(player.getHasGuessedToday());
            assertEquals(initialGuesses + 1, player.getTotalSuccessfulGuesses());
            assertEquals(score, player.getTodayScore());
            verify(repo).save(player);
        }
    }

    @Nested
    class ResetAllScoresTests {

        @Test
        void resetAllScores_callsRepoMethods() {
            // Act
            playerService.resetAllScores();

            // Assert
            verify(repo).resetAllScores();
            verify(repo).flush();
        }
    }

    @Nested
    class UpdatePlayerTests {

        @Test
        void updatePlayer_dataIsValidAndProfileChanged_savesPlayerAndClearsBothCaches() {
            // Arrange
            long playerId = 1L;
            String email = "test@test.com";
            Player existingPlayer = buildPlayer(playerId, email, "test");
            PlayerUpdateRequest request = new PlayerUpdateRequest("TestTest", "AVATAR_DEFAULT");

            Cache classicCache = mock(Cache.class);
            Cache top3Cache = mock(Cache.class);

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of());
            when(repo.existsByLogin("testtest")).thenReturn(false);
            when(cacheManager.getCache("classic-leaderboard")).thenReturn(classicCache);
            when(cacheManager.getCache("top-3-leaderboard")).thenReturn(top3Cache);

            // Act
            PlayerDto returnedDto = playerService.updatePlayer(request, playerId);

            // Assert
            assertEquals("testtest", existingPlayer.getLogin());
            assertEquals("TestTest", existingPlayer.getDisplayName());
            verify(repo).save(existingPlayer);
            verify(classicCache).clear();
            verify(top3Cache).clear();
        }

        @Test
        void updatePlayer_profileNotChangedButInTop3_clearsOnlyTop3Cache() {
            // Arrange
            long playerId = 1L;
            Player existingPlayer = buildPlayer(playerId, "test@test.com", "test");
            PlayerUpdateRequest request = new PlayerUpdateRequest(null, null);

            LeaderboardEntryDto top3Entry = mock(LeaderboardEntryDto.class);
            when(top3Entry.playerId()).thenReturn(playerId);
            Cache top3Cache = mock(Cache.class);

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of(top3Entry));
            when(cacheManager.getCache("top-3-leaderboard")).thenReturn(top3Cache);

            // Act
            playerService.updatePlayer(request, playerId);

            // Assert
            verify(repo).save(existingPlayer);
            verify(top3Cache).clear();
            verify(cacheManager, never()).getCache("classic-leaderboard");
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
            Player existingPlayer = buildPlayer(playerId, "test@test.com", "test");
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
            Player existingPlayer = buildPlayer(playerId, "test@test.com", "test");
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
            Player existingPlayer = buildPlayer(playerId, "test@test.com", "test");
            PlayerUpdateRequest request = new PlayerUpdateRequest("test", "INVALID_AVATAR");

            when(repo.findById(playerId)).thenReturn(Optional.of(existingPlayer));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(request, playerId));
            verify(repo).findById(playerId);
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_callsRepositorySave() {
            // Arrange
            Player player = mock(Player.class);
            when(repo.save(player)).thenReturn(player);

            // Act
            Player result = playerService.save(player);

            // Assert
            assertEquals(player, result);
            verify(repo).save(player);
        }
    }

    @Nested
    class RegisterOAuth2PlayerTests {

        @Test
        void registerOAuth2Player_validData_generatesUniqueLoginAndSavesPlayer() {
            // Arrange
            String rawName = "John Doe";
            String email = "john@example.com";

            when(repo.existsByLogin("john doe")).thenReturn(false);
            when(repo.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Player result = playerService.registerOAuth2Player(email, rawName);

            // Assert
            assertEquals("john doe", result.getLogin());
            assertEquals("John Doe", result.getDisplayName());
            assertEquals("john@example.com", result.getEmail());
            assertEquals(PlayerAuthProvider.OAUTH2_GOOGLE, result.getAuthProvider());
            verify(repo).save(any(Player.class));
        }

        @Test
        void registerOAuth2Player_loginExists_generatesLoginWithCounter() {
            // Arrange
            String rawName = "John";
            String email = "john@example.com";

            when(repo.existsByLogin("john")).thenReturn(true);
            when(repo.existsByLogin("john1")).thenReturn(false);
            when(repo.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Player result = playerService.registerOAuth2Player(email, rawName);

            // Assert
            assertEquals("john1", result.getLogin());
            assertEquals("John", result.getDisplayName());
            verify(repo, times(2)).existsByLogin(anyString());
        }
    }

    @Nested
    class DeletePlayerTests {

        @Test
        void deletePlayer_callsDeletionService() {
            // Arrange
            long playerId = 1L;

            // Act
            playerService.deletePlayer(playerId);

            // Assert
            verify(playerDeletionService).deletePlayerSelf(playerId);
        }
    }

    @Nested
    class ExistsByLoginTests {

        @Test
        void existsByLogin_loginIsNull_returnsFalse() {
            // Act
            Boolean result = playerService.existsByLogin(null);

            // Assert
            assertFalse(result);
            verifyNoInteractions(repo);
        }

        @Test
        void existsByLogin_loginIsValid_callsRepoWithLowercase() {
            // Arrange
            when(repo.existsByLogin("testuser")).thenReturn(true);

            // Act
            Boolean result = playerService.existsByLogin("TestUser");

            // Assert
            assertTrue(result);
        }
    }

    @Nested
    class ExistsByEmailTests {

        @Test
        void existsByEmail_emailIsNull_returnsFalse() {
            // Act
            Boolean result = playerService.existsByEmail(null);

            // Assert
            assertFalse(result);
            verifyNoInteractions(repo);
        }

        @Test
        void existsByEmail_emailIsValid_callsRepoWithLowercase() {
            // Arrange
            when(repo.existsByEmail("test@mail.com")).thenReturn(true);

            // Act
            Boolean result = playerService.existsByEmail("Test@Mail.Com");

            // Assert
            assertTrue(result);
        }
    }
}