package com.bombadle.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class ExcludedRequestsTests {

        @Test
        void doFilterInternal_excludedPath_continuesChain() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/auth/login");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }

        @Test
        void doFilterInternal_unprotectedMethod_continuesChain() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/data");
            when(request.getMethod()).thenReturn("GET");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }
    }

    @Nested
    class ProtectedRequestsTests {

        @Test
        void doFilterInternal_protectedMethodWithValidTokens_continuesChain() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/data");
            when(request.getMethod()).thenReturn("POST");
            when(request.getCookies()).thenReturn(new Cookie[]{validCookie});
            when(request.getHeader("X-XSRF-TOKEN")).thenReturn(validToken);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }

        @Test
        void doFilterInternal_protectedMethodWithoutCookies_sendsForbidden() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/data");
            when(request.getMethod()).thenReturn("POST");
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("X-XSRF-TOKEN")).thenReturn(validToken);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            verifyNoInteractions(filterChain);
        }

        @Test
        void doFilterInternal_protectedMethodWithMissingCookieToken_sendsForbidden() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/data");
            when(request.getMethod()).thenReturn("PUT");
            when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("OTHER_COOKIE", "value")});
            when(request.getHeader("X-XSRF-TOKEN")).thenReturn(validToken);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            verifyNoInteractions(filterChain);
        }

        @Test
        void doFilterInternal_protectedMethodWithMissingHeaderToken_sendsForbidden() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/data");
            when(request.getMethod()).thenReturn("DELETE");
            when(request.getCookies()).thenReturn(new Cookie[]{validCookie});
            when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            verifyNoInteractions(filterChain);
        }

        @Test
        void doFilterInternal_protectedMethodWithMismatchedTokens_sendsForbidden() throws ServletException, IOException {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/data");
            when(request.getMethod()).thenReturn("PATCH");
            when(request.getCookies()).thenReturn(new Cookie[]{validCookie});
            when(request.getHeader("X-XSRF-TOKEN")).thenReturn("different-token");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            verifyNoInteractions(filterChain);
        }
    }
}