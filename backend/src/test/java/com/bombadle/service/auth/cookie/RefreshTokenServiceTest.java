package com.bombadle.service.auth.cookie;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.RefreshToken;
import com.bombadle.enums.Role;
import com.bombadle.repository.RefreshTokenRepository;
import com.bombadle.service.auth.JwtService;
import com.bombadle.service.player.PlayerService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationConfigProperties.JwtConfig jwtConfig;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private Player player;
    private RefreshToken existingToken;
    private final String TEST_EMAIL = "player@example.com";
    private final String RAW_TOKEN = "raw-test-token";
    private final String HASHED_TOKEN = DigestUtils.sha256Hex(RAW_TOKEN);

    @BeforeEach
    void setUp() {
        player = Player.builder()
                .role(Role.ROLE_USER)
                .build();
        player.setEmail(TEST_EMAIL);

        existingToken = RefreshToken.builder()
                .player(player)
                .tokenHash(HASHED_TOKEN)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
    }

    @Nested
    class CreateRefreshTokenTests {

        @Test
        void createRefreshToken_playerExists_returnsDtoAndSavesToken() {
            // Arrange
            when(jwtConfig.refreshExpirationSeconds()).thenReturn(3600L);
            when(playerService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(player));
            when(jwtService.generateJwtToken(any(PlayerPrincipal.class))).thenReturn("mocked-jwt");

            // Act
            RefreshTokenCookieDto result = refreshTokenService.createRefreshToken(TEST_EMAIL);

            // Assert
            assertNotNull(result);
            assertEquals("mocked-jwt", result.getJwt());
            assertNotNull(result.getRefreshToken());
            assertNotNull(result.getExpiresAt());

            verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
            RefreshToken savedToken = refreshTokenCaptor.getValue();
            assertEquals(player, savedToken.getPlayer());
            assertEquals(DigestUtils.sha256Hex(result.getRefreshToken()), savedToken.getTokenHash());
            assertFalse(savedToken.isRevoked());
        }

        @Test
        void createRefreshToken_playerNotFound_throwsException() {
            // Arrange
            when(jwtConfig.refreshExpirationSeconds()).thenReturn(3600L);
            when(playerService.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> refreshTokenService.createRefreshToken(TEST_EMAIL));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    class GetAndRevokeTests {

        @Test
        void getRefreshTokenByToken_tokenExists_returnsToken() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(HASHED_TOKEN)).thenReturn(Optional.of(existingToken));

            // Act
            RefreshToken result = refreshTokenService.getRefreshTokenByToken(RAW_TOKEN);

            // Assert
            assertEquals(existingToken, result);
        }

        @Test
        void getRefreshTokenByToken_tokenNotFound_throwsException() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(HASHED_TOKEN)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(NoSuchElementException.class, () -> refreshTokenService.getRefreshTokenByToken(RAW_TOKEN));
        }

        @Test
        void revokeRefreshToken_validToken_setsRevokedAndSaves() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(HASHED_TOKEN)).thenReturn(Optional.of(existingToken));

            // Act
            refreshTokenService.revokeRefreshToken(RAW_TOKEN);

            // Assert
            assertTrue(existingToken.isRevoked());
            assertNotNull(existingToken.getRevokedAt());
            verify(refreshTokenRepository).save(existingToken);
        }
    }

    @Nested
    class CreateAndRevokeOldTests {

        @Test
        void createRefreshTokenAndRevokeOld_validToken_createsNewAndRevokesOld() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(HASHED_TOKEN)).thenReturn(Optional.of(existingToken));
            when(jwtService.generateJwtToken(any(PlayerPrincipal.class))).thenReturn("mocked-jwt");

            // Act
            RefreshTokenCookieDto result = refreshTokenService.createRefreshTokenAndRevokeOld(RAW_TOKEN);

            // Assert
            assertNotNull(result);
            assertEquals("mocked-jwt", result.getJwt());

            assertTrue(existingToken.isRevoked());
            assertNotNull(existingToken.getRevokedAt());

            verify(refreshTokenRepository, times(2)).save(refreshTokenCaptor.capture());

            RefreshToken savedNewToken = refreshTokenCaptor.getAllValues().get(0);
            RefreshToken updatedOldToken = refreshTokenCaptor.getAllValues().get(1);

            assertEquals(DigestUtils.sha256Hex(result.getRefreshToken()), savedNewToken.getTokenHash());
            assertFalse(savedNewToken.isRevoked());

            assertTrue(updatedOldToken.isRevoked());
        }
    }

    @Nested
    class CleanupTests {

        @Test
        void deleteRevokedRefreshTokens_callsRepositoryWithCorrectTime() {
            // Arrange
            int seconds = 3600;
            when(refreshTokenRepository.deleteRevokedBeforeCutoff(any(Instant.class))).thenReturn(5);

            // Act
            int deletedCount = refreshTokenService.deleteRevokedRefreshTokens(seconds);

            // Assert
            assertEquals(5, deletedCount);
            verify(refreshTokenRepository).deleteRevokedBeforeCutoff(any(Instant.class));
        }
    }
}