package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.entity.AccountRecoveryToken;
import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.Player;
import com.bombadle.entity.VerificationToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.service.player.PlayerCredentialsService;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailActionInitiatorTest {

    @Mock
    private VerificationTokenService tokenService;

    @Mock
    private AccountRecoveryTokenService accountRecoveryTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private ApplicationConfigProperties.EmailConfig emailConfig;

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerCredentialsService playerCredentialsService;

    @Mock
    private DeletedAccountRepository deletedAccountRepository;

    @InjectMocks
    private EmailActionInitiator emailActionInitiator;

    private Player player;
    private VerificationToken token;

    @BeforeEach
    void setUp() {
        player = Player.builder().id(1L).email("test@mail.com").build();
        token = VerificationToken.builder().verificationCode("123456").build();
    }

    @Nested
    class InitiateAccountActivationTests {

        @Test
        void initiateAccountActivationByPlayer_validPlayer_generatesTokenSendsEmailAndRecords() {
            // Arrange
            when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
            when(tokenService.generateNewToken(player, EmailVerificationType.ACCOUNT_ACTIVATION, 15)).thenReturn(token);

            // Act
            emailActionInitiator.initiateAccountActivation(player);

            // Assert
            verify(tokenService).generateNewToken(player, EmailVerificationType.ACCOUNT_ACTIVATION, 15);
            verify(emailService).sendActivationEmail("test@mail.com", "123456");
            verify(playerCredentialsService).recordEmailSent(player.getId());
        }

        @Test
        void initiateAccountActivationByEmail_userExists_callsPlayerOverload() {
            // Arrange
            when(playerService.findByEmail("test@mail.com")).thenReturn(Optional.of(player));
            when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
            when(tokenService.generateNewToken(player, EmailVerificationType.ACCOUNT_ACTIVATION, 15)).thenReturn(token);

            // Act
            emailActionInitiator.initiateAccountActivation("test@mail.com");

            // Assert
            verify(tokenService).generateNewToken(player, EmailVerificationType.ACCOUNT_ACTIVATION, 15);
            verify(emailService).sendActivationEmail("test@mail.com", "123456");
            verify(playerCredentialsService).recordEmailSent(player.getId());
        }

        @Test
        void initiateAccountActivationByEmail_userDoesNotExist_throwsException() {
            // Arrange
            when(playerService.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> emailActionInitiator.initiateAccountActivation("notfound@mail.com"));
            verifyNoInteractions(tokenService, emailService, playerCredentialsService);
        }
    }

    @Nested
    class InitiatePasswordResetTests {

        @Test
        void initiatePasswordReset_userExists_generatesTokenSendsEmailAndRecords() {
            // Arrange
            when(playerService.findByEmail("test@mail.com")).thenReturn(Optional.of(player));
            when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
            when(tokenService.generateNewToken(player, EmailVerificationType.PASSWORD_RESET, 15)).thenReturn(token);

            // Act
            emailActionInitiator.initiatePasswordReset("test@mail.com");

            // Assert
            verify(tokenService).generateNewToken(player, EmailVerificationType.PASSWORD_RESET, 15);
            verify(emailService).sendPasswordResetEmail("test@mail.com", "123456");
            verify(playerCredentialsService).recordEmailSent(player.getId());
        }

        @Test
        void initiatePasswordReset_userDoesNotExist_throwsException() {
            // Arrange
            when(playerService.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> emailActionInitiator.initiatePasswordReset("notfound@mail.com"));
            verifyNoInteractions(tokenService, emailService, playerCredentialsService);
        }
    }

    @Nested
    class InitiateAccountDeletionTests {

        @Test
        void initiateAccountDeletion_validPlayer_generatesTokenSendsEmailAndRecords() {
            // Arrange
            when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
            when(tokenService.generateNewToken(player, EmailVerificationType.ACCOUNT_DELETION, 15)).thenReturn(token);

            // Act
            emailActionInitiator.initiateAccountDeletion(player);

            // Assert
            verify(tokenService).generateNewToken(player, EmailVerificationType.ACCOUNT_DELETION, 15);
            verify(emailService).sendAccountDeletionConfirmationEmail("test@mail.com", "123456");
            verify(playerCredentialsService).recordEmailSent(player.getId());
        }
    }

    @Nested
    class InitiateAccountRecoveryTests {

        @Test
        void initiateAccountRecovery_deletedAccountExists_generatesTokenAndSendsEmail() {
            // Arrange
            DeletedAccount deletedAccount = DeletedAccount.builder().id(5L).email("test@mail.com").build();
            AccountRecoveryToken recoveryToken = AccountRecoveryToken.builder().verificationCode("654321").build();
            when(deletedAccountRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(deletedAccount));
            when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
            when(accountRecoveryTokenService.generateNewToken(5L, EmailVerificationType.ACCOUNT_RECOVERY, 15))
                    .thenReturn(recoveryToken);

            // Act
            emailActionInitiator.initiateAccountRecovery("test@mail.com");

            // Assert
            verify(accountRecoveryTokenService).generateNewToken(5L, EmailVerificationType.ACCOUNT_RECOVERY, 15);
            verify(emailService).sendAccountRecoveryEmail("test@mail.com", "654321");
        }

        @Test
        void initiateAccountRecovery_noDeletedAccount_throwsException() {
            // Arrange
            when(deletedAccountRepository.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> emailActionInitiator.initiateAccountRecovery("notfound@mail.com"));
            verifyNoInteractions(accountRecoveryTokenService, emailService);
        }
    }
}