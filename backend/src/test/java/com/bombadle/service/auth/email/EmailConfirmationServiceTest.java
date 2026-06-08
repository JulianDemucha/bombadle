package com.bombadle.service.auth.email;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.request.PasswordResetRequest;
import com.bombadle.dto.request.VerificationCodeRequest;
import com.bombadle.dto.request.VerificationCodeWithEmailRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.EmailVerificationType;
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
        void confirmEmailVerification_validRequest_activatesAccount() {
            VerificationCodeWithEmailRequest request = new VerificationCodeWithEmailRequest("test@mail.com", "123456");
            when(playerService.findByEmail("test@mail.com")).thenReturn(Optional.of(player));

            emailConfirmationService.confirmEmailVerification(request);

            verify(verificationTokenService).verifyAndConsume(1L, EmailVerificationType.ACCOUNT_ACTIVATION, "123456");
            verify(playerService).activateAccount(1L);
        }

        @Test
        void confirmEmailVerification_playerNotFound_throwsException() {
            VerificationCodeWithEmailRequest request = new VerificationCodeWithEmailRequest("notfound@mail.com", "123456");
            when(playerService.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> emailConfirmationService.confirmEmailVerification(request));
            verifyNoInteractions(verificationTokenService);
        }
    }

    @Nested
    class ConfirmResetPasswordTests {
        @Test
        void confirmResetPassword_validRequest_changesPassword() {
            PasswordResetRequest request = new PasswordResetRequest("test@mail.com", "123456", "newPassword");
            when(playerService.findByEmail("test@mail.com")).thenReturn(Optional.of(player));

            emailConfirmationService.confirmResetPassword(request);

            verify(verificationTokenService).verifyAndConsume(1L, EmailVerificationType.PASSWORD_RESET, "123456");
            verify(playerService).changePassword(1L, "newPassword");
        }

        @Test
        void confirmResetPassword_playerNotFound_throwsException() {
            PasswordResetRequest request = new PasswordResetRequest("notfound@mail.com", "123456", "newPassword");
            when(playerService.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> emailConfirmationService.confirmResetPassword(request));
            verifyNoInteractions(verificationTokenService);
        }
    }

    @Nested
    class ConfirmPlayerSelfDeletionTests {
        @Test
        void confirmPlayerSelfDeletion_validRequest_deletesPlayer() {
            VerificationCodeRequest request = new VerificationCodeRequest("123456");
            PlayerPrincipal principal = mock(PlayerPrincipal.class);
            when(principal.getPlayerId()).thenReturn(1L);

            emailConfirmationService.confirmPlayerSelfDeletion(request, principal);

            verify(verificationTokenService).verifyAndConsume(1L, EmailVerificationType.ACCOUNT_DELETION, "123456");
            verify(playerDeletionService).deletePlayerSelf(1L);
        }
    }
}