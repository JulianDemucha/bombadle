package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.entity.Player;
import com.bombadle.security.oauth2.CustomOAuth2PlayerUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLockedFilterTest {

    @InjectMocks
    private AccountLockedFilter filter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private PlayerPrincipal playerPrincipal;

    @Mock
    private CustomOAuth2PlayerUser customOAuth2PlayerUser;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class ShouldNotFilterTests {

        @Test
        void shouldNotFilter_authPath_returnsTrue() {
            // Arrange
            request.setRequestURI("/api/auth/login");

            // Act & Assert
            assertTrue(filter.shouldNotFilter(request));
        }

        @Test
        void shouldNotFilter_playersMeGet_returnsTrue() {
            // Arrange
            request.setRequestURI("/api/players/me");
            request.setMethod("GET");

            // Act & Assert
            assertTrue(filter.shouldNotFilter(request));
        }

        @Test
        void shouldNotFilter_playersMePost_returnsFalse() {
            // Arrange
            request.setRequestURI("/api/players/me");
            request.setMethod("POST");

            // Act & Assert
            assertFalse(filter.shouldNotFilter(request));
        }

        @Test
        void shouldNotFilter_leaderboardPath_returnsTrue() {
            // Arrange
            request.setRequestURI("/api/leaderboard/top10");

            // Act & Assert
            assertTrue(filter.shouldNotFilter(request));
        }
    }

    @Nested
    class DoFilterInternalTests {

        @Test
        void doFilterInternal_notAuthenticated_continuesChain() throws ServletException, IOException {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(null);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_authenticatedButNotLockedPrincipal_continuesChain() throws ServletException, IOException {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(playerPrincipal);
            when(playerPrincipal.isAccountLocked()).thenReturn(false);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_authenticatedAndLockedPrincipal_returns423() throws ServletException, IOException {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(playerPrincipal);
            when(playerPrincipal.isAccountLocked()).thenReturn(true);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertEquals(423, response.getStatus());
            assertEquals("application/json", response.getContentType());
            assertTrue(response.getContentAsString().contains("Account Locked"));
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        void doFilterInternal_authenticatedAndLockedOAuth2User_returns423() throws ServletException, IOException {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(customOAuth2PlayerUser);
            when(customOAuth2PlayerUser.getPlayer()).thenReturn(Player.builder().accountLocked(true).build());

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertEquals(423, response.getStatus());
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        void doFilterInternal_authenticatedAndNotLockedOAuth2User_continuesChain() throws ServletException, IOException {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(customOAuth2PlayerUser);
            when(customOAuth2PlayerUser.getPlayer()).thenReturn(Player.builder().accountLocked(false).build());

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }
    }
}