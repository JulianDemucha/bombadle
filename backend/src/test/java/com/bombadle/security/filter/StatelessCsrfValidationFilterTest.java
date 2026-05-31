package com.bombadle.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatelessCsrfValidationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StatelessCsrfValidationFilter filter;

    private final String validToken = "valid-csrf-token";
    private final Cookie validCookie = new Cookie("XSRF-TOKEN", validToken);

    @BeforeEach
    void setUp() {
        filter = new StatelessCsrfValidationFilter(List.of("/api/auth", "/public"));
    }

    @Test
    void doFilterInternal_ExcludedPath_ContinuesChain() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void doFilterInternal_UnprotectedMethod_ContinuesChain() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void doFilterInternal_ProtectedMethodWithValidTokens_ContinuesChain() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("POST");
        when(request.getCookies()).thenReturn(new Cookie[]{validCookie});
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(validToken);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void doFilterInternal_ProtectedMethodWithoutCookies_SendsForbidden() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("POST");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(validToken);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_ProtectedMethodWithMissingCookieToken_SendsForbidden() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("PUT");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("OTHER_COOKIE", "value")});
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(validToken);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_ProtectedMethodWithMissingHeaderToken_SendsForbidden() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getCookies()).thenReturn(new Cookie[]{validCookie});
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_ProtectedMethodWithMismatchedTokens_SendsForbidden() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getCookies()).thenReturn(new Cookie[]{validCookie});
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn("different-token");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
        verifyNoInteractions(filterChain);
    }
}