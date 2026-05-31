package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.service.auth.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {


    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PlayerPrincipal playerPrincipal;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetailsService userDetailsService;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private final Cookie dummyJwt = new Cookie("jwt", "jwt");
    private final Cookie[] cookies = new Cookie[]{dummyJwt};
    private final String dummyEmail = "sigma@sigma.sigma";

    @BeforeEach
    void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_unauthenticatedUserWithoutJwtCookie_continuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        filter.doFilterInternal(request, response, filterChain);
        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_authenticatedUser_continuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_unauthenticatedUserWithValidJwtCookie_authenticatesUserAndContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtService.extractEmail(dummyJwt.getValue())).thenReturn(dummyEmail);
        when(userDetailsService.loadUserByUsername(dummyEmail)).thenReturn(playerPrincipal);
        when(jwtService.isTokenValid(dummyJwt.getValue(), playerPrincipal)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(securityContext).setAuthentication(captor.capture());

        UsernamePasswordAuthenticationToken capturedAuth = captor.getValue();

        assertEquals(playerPrincipal, capturedAuth.getPrincipal());
        assertNull(capturedAuth.getCredentials());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_unauthenticatedUserWithInvalidJwtCookie_continuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtService.extractEmail(dummyJwt.getValue())).thenReturn(dummyEmail);
        when(userDetailsService.loadUserByUsername(dummyEmail)).thenReturn(playerPrincipal);
        when(jwtService.isTokenValid(dummyJwt.getValue(), playerPrincipal)).thenReturn(false); //!!

        filter.doFilterInternal(request, response, filterChain);


        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_jwtServiceReturnsNullEmail_continuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtService.extractEmail(dummyJwt.getValue())).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);


        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_JwtThrowsException_CatchesExceptionAndContinuesChain() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);

        when(jwtService.extractEmail(dummyJwt.getValue())).thenThrow(
                new ExpiredJwtException(null, null, "Wygasł")
        );

        filter.doFilterInternal(request, response, filterChain);

        // Weryfikacja
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

}
