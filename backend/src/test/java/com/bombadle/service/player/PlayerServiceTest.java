package com.bombadle.service.player;

import com.bombadle.dto.PlayerDto;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
                .authProvider(PlayerAuthProvider.LOCAL)
                .emailVerified(false)
                .build();
    }

    @Nested
    class FindByEmailTests {
        @Test
        void findByEmail_emailIsNull_returnsEmptyOptional() {
            // Arrange

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
            // Arrange

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
    class GetPlayerByIdTests {
        @Test
        void getPlayerById_userExists_returnsPlayer() {
            // Arrange
            long id = 1L;
            Player player = mock(Player.class);
            when(repo.findById(id)).thenReturn(Optional.of(player));

            // Act
            Player result = playerService.getPlayerById(id);

            // Assert
            assertNotNull(result);
            assertEquals(player, result);
        }

        @Test
        void getPlayerById_userDoesNotExist_throwsUsernameNotFoundException() {
            // Arrange
            long id = 1L;
            when(repo.findById(id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> playerService.getPlayerById(id));
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
            Page<Player> expectedPage = new PageImpl<>(List.of(mock(Player.class)));
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
            Player player = buildPlayer(playerId, "test@gmail.com", "test");
            PlayerDto dto = PlayerDto.toDto(player);

            when(repo.findById(playerId)).thenReturn(Optional.of(player));

            // Act
            PlayerDto returnedDto = playerService.getAuthenticatedPlayerDto(playerId);

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
            assertThrows(UsernameNotFoundException.class, () -> playerService.getAuthenticatedPlayerDto(playerId));
            verify(repo).findById(playerId);
        }
    }

    @Nested
    class FindAllByMarkedForDeletionAtBeforeTests {
        @Test
        void findAllByMarkedForDeletionAtBefore_callsRepo() {
            // Arrange
            Instant cutoff = Instant.now();
            List<Player> expectedList = List.of(mock(Player.class));
            when(repo.findAllByMarkedForDeletionAtBefore(cutoff)).thenReturn(expectedList);

            // Act
            List<Player> result = playerService.findAllByMarkedForDeletionAtBefore(cutoff);

            // Assert
            assertEquals(expectedList, result);
            verify(repo).findAllByMarkedForDeletionAtBefore(cutoff);
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
    class DeletePlayerTests {
        @Test
        void manualDelete_validPlayer_callsRepositoryDelete() {
            // Arrange
            Player player = mock(Player.class);

            // Act
            playerService.manualDelete(player);

            // Assert
            verify(repo).delete(player);
        }
    }

    @Nested
    class ExistsByLoginTests {
        @Test
        void existsByLogin_loginIsNull_returnsFalse() {
            // Arrange

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
            // Arrange

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