package com.bombadle.service.player;

import com.bombadle.dto.request.ChangePasswordRequest;
import com.bombadle.entity.Player;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.PasswordAlreadySetException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerCredentialsServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PlayerCredentialsService playerCredentialsService;

    @Nested
    class ActivateAccountTests {

        @Test
        void activateAccount_userExists_setsVerifiedAndSaves() {
            // Arrange
            Long playerId = 1L;
            Player player = mock(Player.class);
            when(playerService.findById(playerId)).thenReturn(Optional.of(player));

            // Act
            playerCredentialsService.activateAccount(playerId);

            // Assert
            verify(player).setEmailVerified(true);
            verify(playerService).save(player);
        }

        @Test
        void activateAccount_userDoesNotExist_throwsException() {
            // Arrange
            Long playerId = 1L;
            when(playerService.findById(playerId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> playerCredentialsService.activateAccount(playerId));
            verify(playerService, never()).save(any());
        }
    }

    @Nested
    class ChangePasswordTests {

        @Test
        void changePassword_userExists_encodesAndSaves() {
            // Arrange
            Long playerId = 1L;
            String newPassword = "newPass";
            Player player = mock(Player.class);

            when(playerService.findById(playerId)).thenReturn(Optional.of(player));
            when(passwordEncoder.encode(newPassword)).thenReturn("encodedPass");

            // Act
            playerCredentialsService.changePassword(playerId, newPassword);

            // Assert
            verify(player).setPasswordHash("encodedPass");
            verify(playerService).save(player);
        }

        @Test
        void changePassword_userDoesNotExist_throwsException() {
            // Arrange
            Long playerId = 1L;
            when(playerService.findById(playerId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> playerCredentialsService.changePassword(playerId, "newPass"));
            verify(playerService, never()).save(any());
        }
    }

    @Nested
    class ChangePasswordWithVerificationTests {

        @Test
        void changePasswordWithVerification_validCredentials_encodesAndSaves() {
            // Arrange
            Long playerId = 1L;
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");
            Player player = mock(Player.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(player.getPasswordHash()).thenReturn("hashedOldPass");
            when(passwordEncoder.matches("oldPass", "hashedOldPass")).thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("hashedNewPass");

            // Act
            playerCredentialsService.changePasswordWithVerification(playerId, request);

            // Assert
            verify(player).setPasswordHash("hashedNewPass");
            verify(playerService).save(player);
        }

        @Test
        void changePasswordWithVerification_invalidCredentials_throwsException() {
            // Arrange
            Long playerId = 1L;
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass");
            Player player = mock(Player.class);

            when(playerService.getPlayerById(playerId)).thenReturn(player);
            when(player.getPasswordHash()).thenReturn("hashedOldPass");
            when(passwordEncoder.matches("wrongPass", "hashedOldPass")).thenReturn(false);

            // Act
            // Assert
            assertThrows(InvalidCredentialsException.class, () -> playerCredentialsService.changePasswordWithVerification(playerId, request));
            verify(playerService, never()).save(any());
        }
    }

    @Nested
    class SetPasswordIfBlankTests {

        @Test
        void setPasswordIfBlank_passwordIsBlank_setsEncodedPasswordAndSaves() {
            // Arrange
            Long playerId = 1L;
            String newPassword = "newPass";
            Player player = mock(Player.class);

            when(playerService.findById(playerId)).thenReturn(Optional.of(player));
            when(player.getPasswordHash()).thenReturn("");
            when(passwordEncoder.encode(newPassword)).thenReturn("encodedPass");

            // Act
            playerCredentialsService.setPasswordIfBlank(playerId, newPassword);

            // Assert
            verify(player).setPasswordHash("encodedPass");
            verify(playerService).save(player);
        }

        @Test
        void setPasswordIfBlank_passwordIsNotBlank_throwsException() {
            // Arrange
            Long playerId = 1L;
            Player player = mock(Player.class);

            when(playerService.findById(playerId)).thenReturn(Optional.of(player));
            when(player.getPasswordHash()).thenReturn("someHash");

            // Act
            // Assert
            assertThrows(PasswordAlreadySetException.class, () -> playerCredentialsService.setPasswordIfBlank(playerId, "newPass"));
            verify(playerService, never()).save(any());
        }

        @Test
        void setPasswordIfBlank_userDoesNotExist_throwsException() {
            // Arrange
            Long playerId = 1L;
            when(playerService.findById(playerId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(UsernameNotFoundException.class, () -> playerCredentialsService.setPasswordIfBlank(playerId, "newPass"));
        }
    }

    @Nested
    class RecordEmailSentTests {

        @Test
        void recordEmailSent_validPlayerId_callsPlayerServiceUpdate() {
            // Arrange
            Long playerId = 1L;

            // Act
            playerCredentialsService.recordEmailSent(playerId);

            // Assert
            verify(playerService).updateLastEmailSentAt(playerId);
        }
    }
}