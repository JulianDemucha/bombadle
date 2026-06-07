package com.bombadle.service.auth.cookie;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CsrfCookieServiceTest {

    @InjectMocks
    private CsrfCookieService csrfCookieService;

    @Mock
    private ApplicationConfigProperties.CsrfConfig csrfConfig;

    private final Cookie dummyCsrfCookie = new Cookie("XSRF-TOKEN", UUID.randomUUID().toString());

    private MockHttpServletRequest request = new MockHttpServletRequest();
    private MockHttpServletResponse response = new MockHttpServletResponse();

    @Nested
    class EnsureCsrfCookieTests {

        @Test
        void ensureCsrfCookie_hasXsrfCookie_doesNothing() {
            // Arrange
            request.setCookies(dummyCsrfCookie);

            // Act
            csrfCookieService.ensureCsrfCookie(request, response);

            // Assert
            assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        }

        @Test
        void ensureCsrfCookie_noXsrfCookie_setsCookieWithCorrectAttributes() {
            // Arrange
            when(csrfConfig.secure()).thenReturn(true);
            when(csrfConfig.cookieMaxAgeSeconds()).thenReturn(7200L);
            when(csrfConfig.sameSite()).thenReturn("Strict");

            // Act
            csrfCookieService.ensureCsrfCookie(request, response);

            // Assert
            String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
            assertNotNull(setCookieHeader);

            assertTrue(setCookieHeader.startsWith("XSRF-TOKEN="));
            assertTrue(setCookieHeader.contains("Secure"));
            assertTrue(setCookieHeader.contains("Path=/"));
            assertTrue(setCookieHeader.contains("Max-Age=7200"));
            assertTrue(setCookieHeader.contains("SameSite=Strict"));

            assertFalse(setCookieHeader.contains("HttpOnly"));
        }

        @Test
        void ensureCsrfCookie_hasOtherCookiesButNoXsrf_setsXsrfCookie() {
            // Arrange
            request.setCookies(new Cookie("JSESSIONID", "12345ABC"));

            when(csrfConfig.secure()).thenReturn(true);
            when(csrfConfig.cookieMaxAgeSeconds()).thenReturn(3600L);
            when(csrfConfig.sameSite()).thenReturn("Lax");

            // Act
            csrfCookieService.ensureCsrfCookie(request, response);

            // Assert
            assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE));
        }

    }
}