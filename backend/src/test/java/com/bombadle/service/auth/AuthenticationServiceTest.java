package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.dto.request.AuthenticationRequest;
import com.bombadle.dto.request.RegisterRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.exception.UnverifiedEmailException;
import com.bombadle.service.auth.email.EmailActionInitiator;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private PlayerService playerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailActionInitiator emailActionInitiator;

    @Mock
    private ApplicationConfigProperties.EmailConfig emailConfig;

    @Captor
    private ArgumentCaptor<Player> playerCaptor;

    @Nested
    class RegisterTests {

        @Test
        void register_validDataAutoActivateFalse_savesReturnsPlayerAndInitiatesEmail() {
            // Arrange
            RegisterRequest request = new RegisterRequest("TestUser", "Test@Email.com", "password123");

            when(playerService.existsByEmail("test@email.com")).thenReturn(false);
            when(playerService.existsByLogin("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(emailConfig.autoActivateAccount()).thenReturn(false);
            when(playerService.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Player result = authenticationService.register(request);

            // Assert
            verify(playerService).save(playerCaptor.capture());
            Player savedPlayer = playerCaptor.getValue();

            verify(emailActionInitiator).initiateAccountActivation(savedPlayer);

            assertThat(result).isNotNull();
            assertThat(savedPlayer.getDisplayName()).isEqualTo("TestUser");
            assertThat(savedPlayer.getLogin()).isEqualTo("testuser");
            assertThat(savedPlayer.getEmail()).isEqualTo("test@email.com");
            assertThat(savedPlayer.getPasswordHash()).isEqualTo("encodedPassword");
            assertThat(savedPlayer.getRole()).isEqualTo(Role.ROLE_USER);
            assertThat(savedPlayer.getAvatarImage()).isEqualTo(AvatarImage.AVATAR_DEFAULT);
            assertThat(savedPlayer.getAuthProvider()).isEqualTo(PlayerAuthProvider.LOCAL);
            assertThat(savedPlayer.getHasGuessedToday()).isFalse();
            assertThat(savedPlayer.getAccountLocked()).isFalse();
            assertThat(savedPlayer.getEmailVerified()).isFalse();
            assertThat(savedPlayer.getCreatedAt()).isNotNull();
            assertThat(savedPlayer.getLastActiveAt()).isNotNull();
        }

        @Test
        void register_validDataAutoActivateTrue_savesReturnsPlayerAndSkipsEmail() {
            // Arrange
            RegisterRequest request = new RegisterRequest("TestUser", "Test@Email.com", "password123");

            when(playerService.existsByEmail("test@email.com")).thenReturn(false);
            when(playerService.existsByLogin("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(emailConfig.autoActivateAccount()).thenReturn(true);
            when(playerService.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Player result = authenticationService.register(request);

            // Assert
            verify(playerService).save(playerCaptor.capture());
            Player savedPlayer = playerCaptor.getValue();

            verify(emailActionInitiator, never()).initiateAccountActivation(any(Player.class));
            assertThat(savedPlayer.getEmailVerified()).isTrue();
        }

        @Test
        void register_emailExists_throwsRegistrationConflictException() {
            // Arrange
            RegisterRequest request = new RegisterRequest("testuser", "existing@email.com", "password123");
            when(playerService.existsByEmail("existing@email.com")).thenReturn(true);

            // Act
            RegistrationConflictException exception = assertThrows(
                    RegistrationConflictException.class,
                    () -> authenticationService.register(request)
            );

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Email or username already exists");
            verify(playerService, never()).save(any());
        }

        @Test
        void register_usernameExists_throwsRegistrationConflictException() {
            // Arrange
            RegisterRequest request = new RegisterRequest("ExistingUser", "new@email.com", "password123");
            when(playerService.existsByEmail("new@email.com")).thenReturn(false);
            when(playerService.existsByLogin("existinguser")).thenReturn(true);

            // Act
            RegistrationConflictException exception = assertThrows(
                    RegistrationConflictException.class,
                    () -> authenticationService.register(request)
            );

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Email or username already exists");
            verify(playerService, never()).save(any());
        }
    }

    @Nested
    class AuthenticateTests {

        @Test
        void authenticate_validCredentialsAndVerified_authenticatesAndUpdatesLastActiveAt() {
            // Arrange
            AuthenticationRequest request = new AuthenticationRequest("Test@Email.com", "password123");
            Player existingPlayer = Player.builder()
                    .email("test@email.com")
                    .emailVerified(true) // Zmiana dla poprawnego flow
                    .lastActiveAt(Instant.now().minusSeconds(3600))
                    .build();

            when(playerService.findByEmail("test@email.com")).thenReturn(Optional.of(existingPlayer));
            when(playerService.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Player result = authenticationService.authenticate(request);

            // Assert
            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken("test@email.com", "password123")
            );
            verify(playerService).save(playerCaptor.capture());

            Player savedPlayer = playerCaptor.getValue();
            assertThat(result).isNotNull();
            assertThat(savedPlayer.getLastActiveAt()).isAfter(Instant.now().minusSeconds(10));
        }

        @Test
        void authenticate_validCredentialsButUnverified_throwsUnverifiedEmailException() {
            // Arrange
            AuthenticationRequest request = new AuthenticationRequest("test@email.com", "password123");
            Player unverifiedPlayer = Player.builder()
                    .email("test@email.com")
                    .emailVerified(false)
                    .build();

            when(playerService.findByEmail("test@email.com")).thenReturn(Optional.of(unverifiedPlayer));

            // Act
            UnverifiedEmailException exception = assertThrows(
                    UnverifiedEmailException.class,
                    () -> authenticationService.authenticate(request)
            );

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Account isn't verified");
            verify(playerService, never()).save(any());
        }

        @Test
        void authenticate_authenticationManagerFails_throwsInvalidCredentialsException() {
            // Arrange
            AuthenticationRequest request = new AuthenticationRequest("test@email.com", "wrongPassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> authenticationService.authenticate(request)
            );

            // Assert
            assertThat(exception.getMessage()).isEqualTo("Invalid email or password");
            verify(playerService, never()).findByEmail(any());
            verify(playerService, never()).save(any());
        }
    }

    @Nested
    class ExistsTests {

        @Test
        void existsByEmail_emailExists_returnsTrue() {
            // Arrange
            when(playerService.existsByEmail("test@email.com")).thenReturn(true);

            // Act & Assert
            assertThat(authenticationService.existsByEmail("test@email.com")).isTrue();
        }

        @Test
        void existsByUsername_usernameExists_returnsTrue() {
            // Arrange
            when(playerService.existsByLogin("testuser")).thenReturn(true);

            // Act & Assert
            assertThat(authenticationService.existsByUsername("testuser")).isTrue();
        }
    }
}