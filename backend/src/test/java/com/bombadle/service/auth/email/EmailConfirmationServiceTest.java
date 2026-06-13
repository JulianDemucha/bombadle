package com.bombadle.service.auth.email;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.request.PasswordResetRequest;
import com.bombadle.dto.request.VerificationCodeRequest;
import com.bombadle.dto.request.VerificationCodeWithEmailRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.service.player.PlayerCredentialsService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfirmationServiceTest {

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private PlayerDeletionService playerDeletionService;

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerCredentialsService playerCredentialsService;

    @InjectMocks
    private EmailConfirmationService emailConfirmationService;

    private Player player;

    @BeforeEach
    void setUp() {
        player = Player.builder().id(1L).email("test@mail.com").build();
    }

    @Nested
    class ConfirmEmailVerificationTests {

        @Test
        void confirmEmailVerification_validRequest_activatesAccountViaCredentialsService() {
            // Arrange
            VerificationCodeWithEmailRequest request = new VerificationCodeWithEmailRequest("test@mail.com", "123456");
            when(playerService.findByEmail("test@mail.com")).thenReturn(Optional.of(player));

            // Act
            emailConfirmationService.confirmEmailVerification(request);

            // Assert
            verify(verificationTokenService).verifyAndConsume(1L, EmailVerificationType.ACCOUNT_ACTIVATION, "123456");
            verify(playerCredentialsService).activateAccount(1L);
        }

        @Test
        void confirmEmailVerification_playerNotFound_throwsException() {
            // Arrange
            VerificationCodeWithEmailRequest request = new VerificationCodeWithEmailRequest("notfound@mail.com", "123456");
            when(playerService.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> emailConfirmationService.confirmEmailVerification(request));
            verifyNoInteractions(verificationTokenService, playerCredentialsService);
        }
    }

    @Nested
    class ConfirmResetPasswordTests {

        @Test
        void confirmResetPassword_validRequest_changesPasswordViaCredentialsService() {
            // Arrange
            PasswordResetRequest request = new PasswordResetRequest("test@mail.com", "123456", "newPassword");
            when(playerService.findByEmail("test@mail.com")).thenReturn(Optional.of(player));

            // Act
            emailConfirmationService.confirmResetPassword(request);

            // Assert
            verify(verificationTokenService).verifyAndConsume(1L, EmailVerificationType.PASSWORD_RESET, "123456");
            verify(playerCredentialsService).changePassword(1L, "newPassword");
        }

        @Test
        void confirmResetPassword_playerNotFound_throwsException() {
            // Arrange
            PasswordResetRequest request = new PasswordResetRequest("notfound@mail.com", "123456", "newPassword");
            when(playerService.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> emailConfirmationService.confirmResetPassword(request));
            verifyNoInteractions(verificationTokenService, playerCredentialsService);
        }
    }

    @Nested
    class ConfirmPlayerSelfDeletionTests {

        @Test
        void confirmPlayerSelfDeletion_validRequest_deletesPlayer() {
            // Arrange
            VerificationCodeRequest request = new VerificationCodeRequest("123456");
            PlayerPrincipal principal = mock(PlayerPrincipal.class);
            when(principal.getPlayerId()).thenReturn(1L);

            // Act
            emailConfirmationService.confirmPlayerSelfDeletion(request, principal);

            // Assert
            verify(verificationTokenService).verifyAndConsume(1L, EmailVerificationType.ACCOUNT_DELETION, "123456");
            verify(playerDeletionService).deletePlayerSelf(1L);
        }
    }
}