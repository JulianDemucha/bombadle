package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private ApplicationConfigProperties.JwtConfig jwtConfig;

    @Mock
    private UserDetails userDetails;

    private final String TEST_SECRET = "NmU1YzJiY2Q2ZTY2M2Q1ZTIzNWY3ZTU2Y2Y2YTM0OTM1NzRmNTE2NDRhNzQ2NTY1M2IzNDRhNTE3YTM2NTQ2YQ==";
    private final String TEST_EMAIL = "sigma@sigma.sigma";

    @BeforeEach
    void setUp() {
        when(jwtConfig.secret()).thenReturn(TEST_SECRET);
    }

    @Nested
    class TokenGenerationAndExtractionTests {

        @Test
        void generateJwtToken_validUserDetails_createsValidTokenStructure() {
            // Arrange
            when(jwtConfig.expirationSeconds()).thenReturn(3600L);
            when(userDetails.getUsername()).thenReturn(TEST_EMAIL);

            // Act
            String token = jwtService.generateJwtToken(userDetails);

            // Assert
            assertNotNull(token);
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        void extractEmail_validGeneratedToken_returnsCorrectEmail() {
            // Arrange
            when(jwtConfig.expirationSeconds()).thenReturn(3600L);
            when(userDetails.getUsername()).thenReturn(TEST_EMAIL);
            String token = jwtService.generateJwtToken(userDetails);

            // Act
            String extractedEmail = jwtService.extractEmail(token);

            // Assert
            assertEquals(TEST_EMAIL, extractedEmail);
        }
    }

    @Nested
    class TokenValidationTests {

        @Test
        void isTokenValid_tokenIsCorrectAndNotExpired_returnsTrue() {
            // Arrange
            when(jwtConfig.expirationSeconds()).thenReturn(3600L);
            when(userDetails.getUsername()).thenReturn(TEST_EMAIL);
            String token = jwtService.generateJwtToken(userDetails);

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertTrue(isValid);
        }

        @Test
        void isTokenValid_emailDoesNotMatch_returnsFalse() {
            // Arrange
            when(jwtConfig.expirationSeconds()).thenReturn(3600L);
            when(userDetails.getUsername()).thenReturn(TEST_EMAIL);
            String token = jwtService.generateJwtToken(userDetails);

            when(userDetails.getUsername()).thenReturn("not-sigma-hacker@hackers.com");

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertFalse(isValid);
        }

        @Test
        void isTokenValid_tokenIsExpired_returnsFalse() {
            // Arrange
            // USUNIĘTO: when(userDetails.getUsername())...
            byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);

            String expiredToken = Jwts.builder()
                    .setClaims(new HashMap<>())
                    .setSubject(TEST_EMAIL)
                    .setIssuedAt(new Date(System.currentTimeMillis() - 10000L))
                    .setExpiration(new Date(System.currentTimeMillis() - 1000L))
                    .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                    .compact();

            // Act
            boolean isValid = jwtService.isTokenValid(expiredToken, userDetails);

            // Assert
            assertFalse(isValid);
        }

        @Test
        void isTokenValid_tokenIsMalformedOrTampered_returnsFalse() {
            // Arrange
            String malformedToken = "malformed.token.jwt";

            // Act
            boolean isValid = jwtService.isTokenValid(malformedToken, userDetails);

            // Assert
            assertFalse(isValid);
        }
    }
}