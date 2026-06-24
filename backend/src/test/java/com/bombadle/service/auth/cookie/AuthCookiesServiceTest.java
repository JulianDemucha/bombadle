package com.bombadle.service.auth.cookie;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCookiesServiceTest {

    @InjectMocks
    private AuthCookiesService authCookiesService;

    @Mock
    private ApplicationConfigProperties.JwtConfig jwtConfig;

    @Mock
    private CookieService cookieService;

    @Mock
    private HttpServletResponse response;

    @Nested
    class CreateJwtCookieTests {

        @Test
        void createJwtCookie_jwtIsNull_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> authCookiesService.createJwtCookie(null));
        }

        @Test
        void createJwtCookie_jwtIsValid_createsAndReturnsCookie() {
            // Arrange
            long expirationSeconds = 3600L;
            String dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
            ResponseCookie expectedCookie = ResponseCookie.from("jwt", dummyToken).build();

            when(jwtConfig.expirationSeconds()).thenReturn(expirationSeconds);
            when(cookieService.createCookie("jwt", dummyToken, expirationSeconds)).thenReturn(expectedCookie);

            // Act
            ResponseCookie actualCookie = authCookiesService.createJwtCookie(dummyToken);

            // Assert
            assertThat(actualCookie).isEqualTo(expectedCookie);
        }
    }

    @Nested
    class CreateJwtTimerCookieTests {

        @Test
        void createJwtTimerCookie_createsCookieWithCorrectPropertiesAndCalculatedTime() {
            // Arrange
            long expirationSeconds = 3600L; // 1h
            when(jwtConfig.expirationSeconds()).thenReturn(expirationSeconds);
            long expectedTimeLowerBound = System.currentTimeMillis() + (expirationSeconds * 1000L);

            // Act
            ResponseCookie cookie = authCookiesService.createJwtTimerCookie();

            // Assert
            long expectedTimeUpperBound = System.currentTimeMillis() + (expirationSeconds * 1000L);
            assertThat(cookie.getName()).isEqualTo("JWT-EXPIRES-AT");
            assertThat(cookie.isHttpOnly()).isFalse();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(expirationSeconds);
            long actualCookieValue = Long.parseLong(cookie.getValue());
            assertThat(actualCookieValue)
                    .isGreaterThanOrEqualTo(expectedTimeLowerBound)
                    .isLessThanOrEqualTo(expectedTimeUpperBound);
        }
    }

    @Nested
    class CreateRefreshCookieTests {

        @Test
        void createRefreshCookie_refreshTokenIsNull_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> authCookiesService.createRefreshCookie(null));
        }

        @Test
        void createRefreshCookie_refreshToken_createsAndReturnsCookie() {
            // Arrange
            long expirationSeconds = 3600L;
            String dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
            ResponseCookie expectedCookie = ResponseCookie.from("refreshToken", dummyToken).build();

            when(jwtConfig.refreshExpirationSeconds()).thenReturn(expirationSeconds);
            when(cookieService.createCookie("refreshToken", dummyToken, expirationSeconds)).thenReturn(expectedCookie);

            // Act
            ResponseCookie actualCookie = authCookiesService.createRefreshCookie(dummyToken);

            // Assert
            assertThat(actualCookie).isEqualTo(expectedCookie);
        }
    }

    @Nested
    class SetAuthCookiesTests {

        @Test
        void setAuthCookies_validTokens_addsCookiesToResponse() {
            // Arrange
            String jwt = "fakeJwtToken";
            String refreshToken = "fakeRefreshToken";
            long expSeconds = 3600L;
            long refreshExpSeconds = 7200L;

            when(jwtConfig.expirationSeconds()).thenReturn(expSeconds);
            when(jwtConfig.refreshExpirationSeconds()).thenReturn(refreshExpSeconds);

            ResponseCookie mockJwtCookie = ResponseCookie.from("jwt", jwt).build();
            ResponseCookie mockRefreshCookie = ResponseCookie.from("refreshToken", refreshToken).build();

            when(cookieService.createCookie("jwt", jwt, expSeconds)).thenReturn(mockJwtCookie);
            when(cookieService.createCookie("refreshToken", refreshToken, refreshExpSeconds)).thenReturn(mockRefreshCookie);

            // Act
            authCookiesService.setAuthCookies(jwt, refreshToken, response);

            // Assert
            ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

            verify(response, times(3)).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

            List<String> headerNames = headerNameCaptor.getAllValues();
            List<String> headerValues = headerValueCaptor.getAllValues();

            assertThat(headerNames).containsOnly(HttpHeaders.SET_COOKIE);
            assertThat(headerValues.get(0)).isEqualTo(mockJwtCookie.toString());
            assertThat(headerValues.get(1))
                    .startsWith("JWT-EXPIRES-AT=")
                    .contains("Max-Age=" + expSeconds)
                    .contains("Path=/");
            assertThat(headerValues.get(2)).isEqualTo(mockRefreshCookie.toString());
        }
    }

    @Nested
    class CreateClearCookiesHeadersTests {

        @Test
        void createClearCookiesHeaders_createsAndReturnsDeletionCookies() {
            // Arrange
            String[] expectedNames = {"jwt", "refreshToken", "JWT-EXPIRES-AT", "XSRF-TOKEN"};
            for (String name : expectedNames) {
                ResponseCookie deletionCookie = ResponseCookie.from(name, "")
                        .maxAge(0)
                        .build();
                when(cookieService.createDeletionCookie(name)).thenReturn(deletionCookie);
            }

            // Act
            HttpHeaders headers = authCookiesService.createClearCookiesHeaders();

            // Assert
            for (String name : expectedNames) {
                verify(cookieService).createDeletionCookie(name);
            }

            List<String> setCookieHeaders = headers.get(HttpHeaders.SET_COOKIE);
            assertThat(setCookieHeaders).isNotNull().hasSize(4);

            for (String name : expectedNames) {
                assertThat(setCookieHeaders).anyMatch(header -> header.startsWith(name + "=") && header.contains("Max-Age=0"));
            }
        }
    }

    @Nested
    class CreateAnonymousSessionCookieTests {

        @Test
        void createAnonymousSessionCookie_sessionIdIsNull_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> authCookiesService.createAnonymousSessionCookie(null));
        }

        @Test
        void createAnonymousSessionCookie_sessionIdIsValid_createsAndReturnsCookie() {
            // Arrange
            String sessionId = "anon-session-123";
            long expirationSeconds = 60 * 60 * 24L; // 24h
            ResponseCookie expectedCookie = ResponseCookie.from("ANON_SESSION_ID", sessionId).build();

            when(cookieService.createCookie("ANON_SESSION_ID", sessionId, expirationSeconds)).thenReturn(expectedCookie);

            // Act
            ResponseCookie actualCookie = authCookiesService.createAnonymousSessionCookie(sessionId);

            // Assert
            assertThat(actualCookie).isEqualTo(expectedCookie);
            verify(cookieService).createCookie("ANON_SESSION_ID", sessionId, expirationSeconds);
        }
    }
}