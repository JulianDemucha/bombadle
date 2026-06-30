package com.bombadle.service.player;

import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.DeletedAccountStatistic;
import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.repository.DeletedAccountStatisticRepository;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import com.bombadle.service.admin.AdminAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerDeletionServiceTest {

    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private DeletedAccountRepository deletedAccountRepository;
    @Mock
    private DeletedAccountStatisticRepository deletedAccountStatisticRepository;
    @Mock
    private PlayerDailyStatisticRepository playerDailyStatisticRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private PlayerCascadeDeletionService playerCascadeDeletionService;

    @InjectMocks
    private PlayerDeletionService playerDeletionService;

    private Player admin;
    private Player user;

    @BeforeEach
    void setUp() {
        admin = Player.builder().id(1L).role(Role.ROLE_ADMIN).build();
        user = Player.builder().id(2L).role(Role.ROLE_USER).build();
    }

    @Nested
    class MarkForDeletionTests {

        @Test
        void markForDeletion_validAdminAction_setsFlagsAndLogs() {
            // Arrange
            when(playerService.getPlayerById(1L)).thenReturn(admin);
            when(playerService.getPlayerById(2L)).thenReturn(user);

            // Act
            playerDeletionService.markForDeletion(1L, 2L);

            // Assert
            assertTrue(user.getAccountLocked());
            assertNotNull(user.getMarkedForDeletionAt());
            verify(playerService).save(user);
            verify(adminAuditService).logAction(1L, "mark_user_2_for_deletion", null);
        }

        @Test
        void markForDeletion_selfAction_throwsException() {
            // Arrange
            when(playerService.getPlayerById(1L)).thenReturn(admin);

            // Act & Assert
            assertThrows(AdminOperationNotAllowedException.class, () -> playerDeletionService.markForDeletion(1L, 1L));
        }
    }

    @Nested
    class DeleteMarkedForDeletionTests {

        @Test
        void deleteMarkedForDeletion_playersFound_callsCascadeDelete() {
            // Arrange
            Player target = Player.builder().id(3L).build();
            when(playerService.findAllByMarkedForDeletionAtBefore(any(Instant.class))).thenReturn(List.of(target));

            // Act
            playerDeletionService.deleteMarkedForDeletion(Duration.ofDays(1));

            // Assert
            verify(playerCascadeDeletionService).deletePlayerWithCascade(target);
            verify(deletedAccountRepository).save(any());
        }
    }

    @Nested
    class DeletePlayerByAdminTests {

        @Test
        void deletePlayerByAdmin_superadminAction_deletesSuccessfully() {
            // Arrange
            Player superadmin = Player.builder().id(9L).role(Role.ROLE_SUPERADMIN).build();
            when(playerService.getPlayerById(9L)).thenReturn(superadmin);
            when(playerService.getPlayerById(2L)).thenReturn(user);

            // Act
            playerDeletionService.deletePlayerByAdmin(9L, 2L);

            // Assert
            verify(playerCascadeDeletionService).deletePlayerWithCascade(user);
            verify(adminAuditService).logAction(9L, "delete_user_2", null);
        }

        @Test
        void deletePlayerByAdmin_regularAdminAction_throwsAccessDenied() {
            // Arrange
            when(playerService.getPlayerById(1L)).thenReturn(admin);
            when(playerService.getPlayerById(2L)).thenReturn(user);

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> playerDeletionService.deletePlayerByAdmin(1L, 2L));
        }
    }

    @Nested
    class DeletePlayerSelfTests {

        @Test
        void deletePlayerSelf_deleteAllDataNowTrue_skipsSnapshotAndCascades() {
            // Arrange
            when(playerService.getPlayerById(2L)).thenReturn(user);

            // Act
            playerDeletionService.deletePlayerSelf(2L, true);

            // Assert
            verify(playerCascadeDeletionService).deletePlayerWithCascade(user);
            verifyNoInteractions(deletedAccountRepository, deletedAccountStatisticRepository, playerDailyStatisticRepository);
        }

        @Test
        void deletePlayerSelf_deleteAllDataNowFalse_snapshotsBeforeCascade() {
            // Arrange
            when(playerService.getPlayerById(2L)).thenReturn(user);

            DeletedAccount savedSnapshot = DeletedAccount.builder().id(42L).build();
            when(deletedAccountRepository.save(any())).thenReturn(savedSnapshot);

            PlayerDailyStatistic classicSolve1 = PlayerDailyStatistic.builder()
                    .gameMode(GameMode.CLASSIC)
                    .puzzleDate(java.time.LocalDate.of(2026, 1, 1))
                    .solvedAt(Instant.parse("2026-01-01T10:00:00Z"))
                    .numberOfTries(3)
                    .leaderboardPosition(1)
                    .totalParticipants(50)
                    .build();
            PlayerDailyStatistic classicSolve2 = PlayerDailyStatistic.builder()
                    .gameMode(GameMode.CLASSIC)
                    .puzzleDate(java.time.LocalDate.of(2026, 1, 2))
                    .solvedAt(Instant.parse("2026-01-02T10:00:00Z"))
                    .numberOfTries(5)
                    .leaderboardPosition(10)
                    .totalParticipants(50)
                    .build();
            PlayerDailyStatistic imagesSolve = PlayerDailyStatistic.builder()
                    .gameMode(GameMode.IMAGES)
                    .puzzleDate(java.time.LocalDate.of(2026, 1, 1))
                    .solvedAt(Instant.parse("2026-01-01T11:00:00Z"))
                    .numberOfTries(1)
                    .leaderboardPosition(2)
                    .totalParticipants(50)
                    .build();
            when(playerDailyStatisticRepository.findAllByPlayerId(2L))
                    .thenReturn(List.of(classicSolve1, classicSolve2, imagesSolve));

            // Act
            playerDeletionService.deletePlayerSelf(2L, false);

            // Assert
            ArgumentCaptor<DeletedAccount> accountCaptor = ArgumentCaptor.forClass(DeletedAccount.class);
            verify(deletedAccountRepository).save(accountCaptor.capture());
            assertEquals(2L, accountCaptor.getValue().getOriginalPlayerId());
            assertEquals(2L, accountCaptor.getValue().getDeletedByActorId());

            ArgumentCaptor<DeletedAccountStatistic> statCaptor = ArgumentCaptor.forClass(DeletedAccountStatistic.class);
            verify(deletedAccountStatisticRepository).save(statCaptor.capture());
            DeletedAccountStatistic statistic = statCaptor.getValue();
            assertEquals(42L, statistic.getDeletedAccountId());
            assertEquals(2, statistic.getGuessesByMode().get(GameMode.CLASSIC));
            assertEquals(1, statistic.getGuessesByMode().get(GameMode.IMAGES));
            assertEquals(2, statistic.getTotalTop3Finishes());
            assertEquals(3, statistic.getDailyStatisticsSnapshot().size());

            InOrder inOrder = inOrder(playerDailyStatisticRepository, deletedAccountStatisticRepository, playerCascadeDeletionService);
            inOrder.verify(playerDailyStatisticRepository).findAllByPlayerId(2L);
            inOrder.verify(deletedAccountStatisticRepository).save(any());
            inOrder.verify(playerCascadeDeletionService).deletePlayerWithCascade(user);
        }
    }

    @Nested
    class PurgeExpiredDeletedAccountSnapshotsTests {

        @Test
        void purgeExpiredDeletedAccountSnapshots_expiredFound_deletesStatisticsThenAccounts() {
            // Arrange
            DeletedAccount expired1 = DeletedAccount.builder().id(10L).build();
            DeletedAccount expired2 = DeletedAccount.builder().id(11L).build();
            when(deletedAccountRepository.findAllByDeletedAtBefore(any(Instant.class)))
                    .thenReturn(List.of(expired1, expired2));

            // Act
            playerDeletionService.purgeExpiredDeletedAccountSnapshots(Duration.ofDays(7));

            // Assert
            verify(deletedAccountStatisticRepository).deleteAllByDeletedAccountIdIn(List.of(10L, 11L));
            verify(deletedAccountRepository).deleteAll(List.of(expired1, expired2));
        }

        @Test
        void purgeExpiredDeletedAccountSnapshots_noneExpired_doesNothing() {
            // Arrange
            when(deletedAccountRepository.findAllByDeletedAtBefore(any(Instant.class))).thenReturn(List.of());

            // Act
            playerDeletionService.purgeExpiredDeletedAccountSnapshots(Duration.ofDays(7));

            // Assert
            verifyNoInteractions(deletedAccountStatisticRepository);
            verify(deletedAccountRepository, never()).deleteAll(any());
        }
    }
}