package com.bombadle.service.player;

import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.Player;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.*;
import com.bombadle.service.admin.AdminAuditService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerDeletionServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private GuessListRepository guessListRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ScoreRepository scoreRepository;
    @Mock
    private DeletedAccountRepository deletedAccountRepository;

    @InjectMocks
    private PlayerDeletionService playerDeletionService;

    @Nested
    class MarkForDeletionTests {

        @Test
        void markForDeletion_actorNotFound_throwsUsernameNotFoundException() {
            // Arrange
            when(playerRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> playerDeletionService.markForDeletion(1L, 2L));
            verify(playerRepository, never()).save(any());
        }

        @Test
        void markForDeletion_actorIsTarget_throwsAdminOperationNotAllowedException() {
            // Arrange
            Player actor = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));

            // Act & Assert
            assertThrows(AdminOperationNotAllowedException.class, () -> playerDeletionService.markForDeletion(1L, 1L));
        }

        @Test
        void markForDeletion_adminModifiesAnotherAdminOrSuperadmin_throwsAccessDeniedException() {
            // Arrange
            Player actor = mock(Player.class);
            Player target = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(actor.getRole()).thenReturn(Role.ROLE_ADMIN);
            when(target.getRole()).thenReturn(Role.ROLE_ADMIN);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(target));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> playerDeletionService.markForDeletion(1L, 2L));
        }

        @Test
        void markForDeletion_validAdminAction_updatesTargetAndLogs() {
            // Arrange
            Player actor = mock(Player.class);
            Player target = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(actor.getRole()).thenReturn(Role.ROLE_ADMIN);
            when(target.getRole()).thenReturn(Role.ROLE_USER);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(target));

            // Act
            playerDeletionService.markForDeletion(1L, 2L);

            // Assert
            verify(target).setMarkedForDeletionAt(any(Instant.class));
            verify(target).setAccountLocked(true);
            verify(playerRepository).save(target);
            verify(adminAuditService).logAction(1L, "mark_user_2_for_deletion", null);
        }
    }

    @Nested
    class CancelDeletionTests {

        @Test
        void cancelDeletion_validAdminAction_resetsTargetAndLogs() {
            // Arrange
            Player actor = mock(Player.class);
            Player target = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(actor.getRole()).thenReturn(Role.ROLE_ADMIN);
            when(target.getRole()).thenReturn(Role.ROLE_USER);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(target));

            // Act
            playerDeletionService.cancelDeletion(1L, 2L);

            // Assert
            verify(target).setMarkedForDeletionAt(null);
            verify(target).setAccountLocked(false);
            verify(playerRepository).save(target);
            verify(adminAuditService).logAction(1L, "cancel_mark_user_2", null);
        }
    }

    @Nested
    class DeleteMarkedForDeletionTests {

        @Test
        void deleteMarkedForDeletion_noPlayersFound_doesNothing() {
            // Arrange
            when(playerRepository.findAllByMarkedForDeletionAtBefore(any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            playerDeletionService.deleteMarkedForDeletion(Duration.ofDays(7));

            // Assert
            verifyNoInteractions(guessListRepository, refreshTokenRepository, scoreRepository, deletedAccountRepository, adminAuditService);
            verify(playerRepository, never()).delete(any());
        }

        @Test
        void deleteMarkedForDeletion_playersFound_deletesWithoutAuditLog() {
            // Arrange
            Player target = mock(Player.class);
            when(target.getId()).thenReturn(5L);
            when(playerRepository.findAllByMarkedForDeletionAtBefore(any(Instant.class)))
                    .thenReturn(List.of(target));

            // Act
            playerDeletionService.deleteMarkedForDeletion(Duration.ofDays(7));

            // Assert
            verify(deletedAccountRepository).save(any(DeletedAccount.class));
            verify(guessListRepository).deleteByPlayerId(5L);
            verify(refreshTokenRepository).deleteByPlayerId(5L);
            verify(scoreRepository).deleteByPlayerId(5L);
            verify(playerRepository).delete(target);
            verifyNoInteractions(adminAuditService);
        }
    }

    @Nested
    class DeletePlayerSelfTests {

        @Test
        void deletePlayerSelf_playerExists_deletesEverythingAndLogs() {
            // Arrange
            Player target = mock(Player.class);
            when(target.getId()).thenReturn(10L);
            when(playerRepository.findById(10L)).thenReturn(Optional.of(target));

            // Act
            playerDeletionService.deletePlayerSelf(10L);

            // Assert
            verify(deletedAccountRepository).save(any(DeletedAccount.class));
            verify(guessListRepository).deleteByPlayerId(10L);
            verify(refreshTokenRepository).deleteByPlayerId(10L);
            verify(scoreRepository).deleteByPlayerId(10L);
            verify(playerRepository).delete(target);
            verify(adminAuditService).logAction(10L, "delete_user_self_10", null);
        }
    }

    @Nested
    class DeletePlayerByAdminTests {

        @Test
        void deletePlayerByAdmin_actorNotSuperadmin_throwsAccessDeniedException() {
            // Arrange
            Player actor = mock(Player.class);
            Player target = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(actor.getRole()).thenReturn(Role.ROLE_ADMIN);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(target));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> playerDeletionService.deletePlayerByAdmin(1L, 2L));
        }

        @Test
        void deletePlayerByAdmin_targetIsSuperadmin_throwsAccessDeniedException() {
            // Arrange
            Player actor = mock(Player.class);
            Player target = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(actor.getRole()).thenReturn(Role.ROLE_SUPERADMIN);
            when(target.getRole()).thenReturn(Role.ROLE_SUPERADMIN);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(target));

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> playerDeletionService.deletePlayerByAdmin(1L, 2L));
        }

        @Test
        void deletePlayerByAdmin_validSuperadminAction_deletesTargetAndLogs() {
            // Arrange
            Player actor = mock(Player.class);
            Player target = mock(Player.class);
            when(actor.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);
            when(actor.getRole()).thenReturn(Role.ROLE_SUPERADMIN);
            when(target.getRole()).thenReturn(Role.ROLE_USER);

            when(playerRepository.findById(1L)).thenReturn(Optional.of(actor));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(target));

            // Act
            playerDeletionService.deletePlayerByAdmin(1L, 2L);

            // Assert
            verify(deletedAccountRepository).save(any(DeletedAccount.class));
            verify(guessListRepository).deleteByPlayerId(2L);
            verify(refreshTokenRepository).deleteByPlayerId(2L);
            verify(scoreRepository).deleteByPlayerId(2L);
            verify(playerRepository).delete(target);
            verify(adminAuditService).logAction(1L, "delete_user_2", null);
        }
    }
}