package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminUserUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private Player userTarget;

    @BeforeEach
    void setUp() {
        adminActor = Player.builder().id(1L).role(Role.ROLE_ADMIN).build();
        userTarget = Player.builder().id(3L).role(Role.ROLE_USER).build();
    }

    @Nested
    class UpdateUserTests {

        @Test
        void updateUser_changeLogin_updatesLoginLogsAndClearsCache() {
            // ARRANGE
            AdminUserUpdateRequest request = new AdminUserUpdateRequest("sigma_login", null, null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));
            when(playerRepository.existsByLogin("sigma_login")).thenReturn(false);

            // ACT
            adminUserService.updateUser(1L, 3L, request);

            // ASSERT
            assertEquals("sigma_login", userTarget.getLogin());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(eq(1L), contains("_change_login_to_sigma_login"), isNull());

            verify(cacheService).clear("full-leaderboard");
            verify(cacheService).clear("top-3-leaderboard");
        }

        @Test
        void updateUser_changeAvatar_updatesAvatarLogsAndClearsCache() {
            // ARRANGE
            AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, "AVATAR_BOMBA", null, null);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(adminActor));
            when(playerRepository.findById(3L)).thenReturn(Optional.of(userTarget));

            // ACT
            adminUserService.updateUser(1L, 3L, request);

            // ASSERT
            assertEquals(AvatarImage.AVATAR_BOMBA, userTarget.getAvatarImage());
            verify(playerRepository).save(userTarget);
            verify(adminAuditService).logAction(eq(1L), contains("_change_avatar_to_AVATAR_BOMBA"), isNull());

            verify(cacheService).clear("full-leaderboard");
            verify(cacheService).clear("top-3-leaderboard");
        }
    }
}
