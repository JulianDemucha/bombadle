package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.security.oauth2.CustomOAuth2PlayerUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
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

    @Test
    void shouldNotFilter_AuthPath_ReturnsTrue() {
        request.setRequestURI("/api/auth/login");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_PlayersMeGet_ReturnsTrue() {
        request.setRequestURI("/api/players/me");
        request.setMethod("GET");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_PlayersMePost_ReturnsFalse() {
        request.setRequestURI("/api/players/me");
        request.setMethod("POST");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_LeaderboardPath_ReturnsTrue() {
        request.setRequestURI("/api/leaderboard/top10");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_NotAuthenticated_ContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthenticatedButNotLockedPrincipal_ContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(playerPrincipal);
        when(playerPrincipal.isAccountLocked()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthenticatedAndLockedPrincipal_Returns423() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(playerPrincipal);
        when(playerPrincipal.isAccountLocked()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(423, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Account Locked"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_AuthenticatedAndLockedOAuth2User_Returns423() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customOAuth2PlayerUser);
        when(customOAuth2PlayerUser.getPlayer().getAccountLocked()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(423, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_AuthenticatedAndNotLockedOAuth2User_ContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customOAuth2PlayerUser);
        when(customOAuth2PlayerUser.getPlayer().getAccountLocked()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}