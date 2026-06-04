package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminUserUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private CacheService cacheService;

    private Player adminActor;
    private Player superAdminActor;
    private Player userTarget;

    @BeforeEach
    void setUp() {
        adminActor = Player.builder().id(1L).role(Role.ROLE_ADMIN).build();
        superAdminActor = Player.builder().id(2L).role(Role.ROLE_SUPERADMIN).build();
        userTarget = Player.builder().id(3L).role(Role.ROLE_USER).build();
    }

    @Nested
    class BlockUserTests {

        @Test
        void blockUser_validAction_blocksUserAndLogs() {
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.blockUser(1L, 3L);

            assertTrue(userTarget.getAccountLocked());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(1L, "block_user_3", null);
        }

        @Test
        void blockUser_selfBlock_throwsException() {
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));

            assertThrows(AdminOperationNotAllowedException.class, () ->
                    adminUserService.blockUser(1L, 1L)
            );
        }

        @Test
        void blockUser_adminOnAdmin_throwsException() {
            Player anotherAdmin = Player.builder().id(4L).role(Role.ROLE_ADMIN).build();
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(4L)).thenReturn(Optional.of(anotherAdmin));

            assertThrows(AccessDeniedException.class, () ->
                    adminUserService.blockUser(1L, 4L)
            );
        }
    }

    @Nested
    class UnblockUserTests {

        @Test
        void unblockUser_validAction_unblocksUserAndLogs() {
            userTarget.setAccountLocked(true);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.unblockUser(1L, 3L);

            assertFalse(userTarget.getAccountLocked());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(1L, "unblock_user_3", null);
        }

        @Test
        void unblockUser_targetMarkedForDeletion_throwsException() {
            userTarget.setMarkedForDeletionAt(Instant.now());
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            assertThrows(AdminOperationNotAllowedException.class, () ->
                    adminUserService.unblockUser(1L, 3L)
            );
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void updateUser_noChanges_doesNothing() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, null, null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.updateUser(1L, 3L, request);

            verify(playerRepository, never()).save(any());
            verify(adminAuditService, never()).logAction(anyLong(), anyString(), any());
        }

        @Test
        void updateUser_changeLogin_updatesLoginAndLogs() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest("sigma_login", null, null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));
            when(playerRepository.existsByLogin("sigma_login")).thenReturn(false);

            adminUserService.updateUser(1L, 3L, request);

            assertEquals("sigma_login", userTarget.getLogin());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(eq(1L), contains("_change_login_to_sigma_login"), isNull());
        }

        @Test
        void updateUser_changeAvatar_updatesAvatarAndLogs() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, "AVATAR_BOMBA", null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.updateUser(1L, 3L, request);

            assertEquals(AvatarImage.AVATAR_BOMBA, userTarget.getAvatarImage());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(eq(1L), contains("_change_avatar_to_AVATAR_BOMBA"), isNull());
        }

        @Test
        void updateUser_clearScoreWhenGuessed_clearsAndLogs() {
            userTarget.setHasGuessedToday(true);
            userTarget.setTotalSuccessfulGuesses(5);
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, null, null, true);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.updateUser(1L, 3L, request);

            assertFalse(userTarget.getHasGuessedToday());
            assertNull(userTarget.getTodayScore());
            assertEquals(4, userTarget.getTotalSuccessfulGuesses());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(eq(1L), contains("_clear_today_score"), isNull());
        }

        @Test
        void updateUser_clearScoreWhenNotGuessed_doesNothing() {
            userTarget.setHasGuessedToday(false);
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, null, null, true);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.updateUser(1L, 3L, request);

            verify(playerRepository, never()).save(any());
            verify(adminAuditService, never()).logAction(anyLong(), anyString(), any());
        }

        @Test
        void updateUser_changeWins_updatesWinsAndLogs() {
            userTarget.setTotalSuccessfulGuesses(5);
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, null, 10, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            adminUserService.updateUser(1L, 3L, request);

            assertEquals(10, userTarget.getTotalSuccessfulGuesses());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(eq(1L), contains("_change_wins_to_10"), isNull());
        }

        @Test
        void updateUser_negativeWins_throwsException() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, null, -5, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    adminUserService.updateUser(1L, 3L, request)
            );

            assertEquals("totalSuccessfulGuesses must be >= 0", exception.getMessage());
            verify(playerRepository, never()).save(any());
        }

        @Test
        void updateUser_loginTooShort_throwsException() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest("si", null, null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    adminUserService.updateUser(1L, 3L, request)
            );

            assertEquals("Username must be between 3 and 16 characters", exception.getMessage());
        }

        @Test
        void updateUser_loginTooLong_throwsException() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest("sigma_login_too_long_for_db", null, null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    adminUserService.updateUser(1L, 3L, request)
            );

            assertEquals("Username must be between 3 and 16 characters", exception.getMessage());
        }

        @Test
        void updateUser_loginAlreadyExists_throwsException() {
            AdminUserUpdateRequest request = new AdminUserUpdateRequest("beta_login", null, null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));
            when(playerRepository.existsByLogin("beta_login")).thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    adminUserService.updateUser(1L, 3L, request)
            );

            assertEquals("Username beta_login already exists", exception.getMessage());
        }
    }
}