package com.bombadle.service.player;

import com.bombadle.entity.Player;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.service.admin.AdminAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        void deletePlayerSelf_validCall_deletesSelf() {
            // Arrange
            when(playerService.getPlayerById(2L)).thenReturn(user);

            // Act
            playerDeletionService.deletePlayerSelf(2L);

            // Assert
            verify(playerCascadeDeletionService).deletePlayerWithCascade(user);
            verify(deletedAccountRepository).save(any());
        }
    }
}