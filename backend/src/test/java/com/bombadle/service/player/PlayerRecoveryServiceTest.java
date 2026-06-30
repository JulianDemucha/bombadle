package com.bombadle.service.player;

import com.bombadle.dto.DailyStatisticSnapshot;
import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.DeletedAccountStatistic;
import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.repository.DeletedAccountStatisticRepository;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerRecoveryServiceTest {

    @Mock
    private PlayerService playerService;
    @Mock
    private DeletedAccountRepository deletedAccountRepository;
    @Mock
    private DeletedAccountStatisticRepository deletedAccountStatisticRepository;
    @Mock
    private PlayerDailyStatisticRepository playerDailyStatisticRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PlayerRecoveryService playerRecoveryService;

    private DeletedAccount localDeletedAccount;

    @BeforeEach
    void setUp() {
        localDeletedAccount = DeletedAccount.builder()
                .id(7L)
                .originalPlayerId(2L)
                .login("kapitanbomba")
                .email("bomba@mail.com")
                .role(Role.ROLE_USER)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .totalSuccessfulGuesses(15)
                .avatarImage(AvatarImage.AVATAR_BOMBA)
                .authProvider(PlayerAuthProvider.LOCAL)
                .deletedAt(Instant.now())
                .deletedByActorId(2L)
                .build();

        lenient().when(playerService.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class ConflictTests {

        @Test
        void recoverAccount_loginTaken_throwsRegistrationConflictException() {
            when(playerService.existsByLogin("kapitanbomba")).thenReturn(true);

            assertThrows(RegistrationConflictException.class,
                    () -> playerRecoveryService.recoverAccount(localDeletedAccount, "newPass123"));

            verify(playerService, never()).save(any());
            verify(deletedAccountRepository, never()).delete(any());
        }

        @Test
        void recoverAccount_emailTaken_throwsRegistrationConflictException() {
            when(playerService.existsByLogin("kapitanbomba")).thenReturn(false);
            when(playerService.existsByEmail("bomba@mail.com")).thenReturn(true);

            assertThrows(RegistrationConflictException.class,
                    () -> playerRecoveryService.recoverAccount(localDeletedAccount, "newPass123"));

            verify(playerService, never()).save(any());
        }
    }

    @Nested
    class PasswordRequirementTests {

        @Test
        void recoverAccount_localWithoutPassword_throwsIllegalArgumentException() {
            when(playerService.existsByLogin("kapitanbomba")).thenReturn(false);
            when(playerService.existsByEmail("bomba@mail.com")).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> playerRecoveryService.recoverAccount(localDeletedAccount, null));

            verify(playerService, never()).save(any());
        }

        @Test
        void recoverAccount_localWithBlankPassword_throwsIllegalArgumentException() {
            when(playerService.existsByLogin("kapitanbomba")).thenReturn(false);
            when(playerService.existsByEmail("bomba@mail.com")).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> playerRecoveryService.recoverAccount(localDeletedAccount, "   "));

            verify(playerService, never()).save(any());
        }

        @Test
        void recoverAccount_oauthGoogleWithoutPassword_succeedsWithBlankPasswordHash() {
            DeletedAccount googleAccount = DeletedAccount.builder()
                    .id(8L)
                    .login("googleuser")
                    .email("google@mail.com")
                    .role(Role.ROLE_USER)
                    .createdAt(Instant.now())
                    .totalSuccessfulGuesses(0)
                    .avatarImage(AvatarImage.AVATAR_DEFAULT)
                    .authProvider(PlayerAuthProvider.OAUTH2_GOOGLE)
                    .deletedAt(Instant.now())
                    .build();
            when(playerService.existsByLogin("googleuser")).thenReturn(false);
            when(playerService.existsByEmail("google@mail.com")).thenReturn(false);
            when(deletedAccountStatisticRepository.findByDeletedAccountId(8L)).thenReturn(Optional.empty());

            Player result = playerRecoveryService.recoverAccount(googleAccount, null);

            assertEquals("", result.getPasswordHash());
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    class SuccessfulRecoveryTests {

        @Test
        void recoverAccount_localWithStatistic_restoresProfileStreaksAndDailyHistory() {
            // Arrange
            when(playerService.existsByLogin("kapitanbomba")).thenReturn(false);
            when(playerService.existsByEmail("bomba@mail.com")).thenReturn(false);
            when(passwordEncoder.encode("newPass123")).thenReturn("encoded-hash");

            DailyStatisticSnapshot snapshot1 = new DailyStatisticSnapshot(
                    GameMode.CLASSIC, LocalDate.of(2026, 1, 1), Instant.parse("2026-01-01T10:00:00Z"), 3, 1, 50);
            DailyStatisticSnapshot snapshot2 = new DailyStatisticSnapshot(
                    GameMode.IMAGES, LocalDate.of(2026, 1, 2), Instant.parse("2026-01-02T10:00:00Z"), 2, 5, 50);

            DeletedAccountStatistic statistic = DeletedAccountStatistic.builder()
                    .deletedAccountId(7L)
                    .currentStreak(4)
                    .longestStreak(9)
                    .currentSuperstreak(1)
                    .longestSuperstreak(2)
                    .totalSuccessfulGuesses(15)
                    .dailyStatisticsSnapshot(List.of(snapshot1, snapshot2))
                    .capturedAt(Instant.now())
                    .build();
            when(deletedAccountStatisticRepository.findByDeletedAccountId(7L)).thenReturn(Optional.of(statistic));

            // Act
            Player result = playerRecoveryService.recoverAccount(localDeletedAccount, "newPass123");

            // Assert profile fields
            assertEquals("kapitanbomba", result.getLogin());
            assertEquals("bomba@mail.com", result.getEmail());
            assertEquals(Role.ROLE_USER, result.getRole());
            assertEquals(AvatarImage.AVATAR_BOMBA, result.getAvatarImage());
            assertEquals(PlayerAuthProvider.LOCAL, result.getAuthProvider());
            assertEquals(15, result.getTotalSuccessfulGuesses());
            assertEquals("encoded-hash", result.getPasswordHash());
            assertTrue(result.getEmailVerified());
            assertFalse(result.getAccountLocked());
            assertNull(result.getMarkedForDeletionAt());
            assertEquals(4, result.getCurrentStreak());
            assertEquals(9, result.getLongestStreak());
            assertEquals(1, result.getCurrentSuperstreak());
            assertEquals(2, result.getLongestSuperstreak());
            assertTrue(result.getCompletedModesToday().isEmpty());

            // Assert daily history replay
            ArgumentCaptor<List<PlayerDailyStatistic>> restoredCaptor = ArgumentCaptor.forClass(List.class);
            verify(playerDailyStatisticRepository).saveAll(restoredCaptor.capture());
            List<PlayerDailyStatistic> restored = restoredCaptor.getValue();
            assertEquals(2, restored.size());
            assertEquals(GameMode.CLASSIC, restored.get(0).getGameMode());
            assertEquals(result, restored.get(0).getPlayer());
            assertEquals(GameMode.IMAGES, restored.get(1).getGameMode());

            // Assert snapshot cleanup
            verify(deletedAccountStatisticRepository).delete(statistic);
            verify(deletedAccountRepository).delete(localDeletedAccount);

            InOrder inOrder = inOrder(playerService, playerDailyStatisticRepository, deletedAccountStatisticRepository, deletedAccountRepository);
            inOrder.verify(playerService).save(any(Player.class));
            inOrder.verify(playerDailyStatisticRepository).saveAll(any());
            inOrder.verify(deletedAccountStatisticRepository).delete(statistic);
            inOrder.verify(deletedAccountRepository).delete(localDeletedAccount);
        }

        @Test
        void recoverAccount_noStatisticFound_defaultsStreaksToZeroAndSkipsHistoryRestore() {
            // Arrange
            when(playerService.existsByLogin("kapitanbomba")).thenReturn(false);
            when(playerService.existsByEmail("bomba@mail.com")).thenReturn(false);
            when(passwordEncoder.encode("newPass123")).thenReturn("encoded-hash");
            when(deletedAccountStatisticRepository.findByDeletedAccountId(7L)).thenReturn(Optional.empty());

            // Act
            Player result = playerRecoveryService.recoverAccount(localDeletedAccount, "newPass123");

            // Assert
            assertEquals(0, result.getCurrentStreak());
            assertEquals(0, result.getLongestStreak());
            assertEquals(0, result.getCurrentSuperstreak());
            assertEquals(0, result.getLongestSuperstreak());
            verify(playerDailyStatisticRepository, never()).saveAll(any());
            verify(deletedAccountStatisticRepository, never()).delete(any(DeletedAccountStatistic.class));
            verify(deletedAccountRepository).delete(localDeletedAccount);
        }
    }
}
