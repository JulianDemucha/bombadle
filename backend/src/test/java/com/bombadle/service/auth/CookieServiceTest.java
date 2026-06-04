package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    @Mock
    private ApplicationConfigProperties.CookieConfig cookieConfig;

    @InjectMocks
    private CookieService cookieService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Captor
    private ArgumentCaptor<String> headerNameCaptor;

    @Captor
    private ArgumentCaptor<String> headerValueCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(cookieConfig.httpOnly()).thenReturn(true);
        lenient().when(cookieConfig.secure()).thenReturn(true);
        lenient().when(cookieConfig.sameSite()).thenReturn("Strict");
    }

    @Nested
    class CreateCookieTests {

        @Test
        void createCookie_validInput_returnsConfiguredCookie() {
            // Act
            ResponseCookie result = cookieService.createCookie("jwtToken", "dummyValue", 3600);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("jwtToken");
            assertThat(result.getValue()).isEqualTo("dummyValue");
            assertThat(result.getMaxAge().getSeconds()).isEqualTo(3600);
            assertThat(result.getPath()).isEqualTo("/");
            assertThat(result.isHttpOnly()).isTrue();
            assertThat(result.isSecure()).isTrue();
            assertThat(result.getSameSite()).isEqualTo("Strict");
        }
    }

    @Nested
    class DeleteCookieTests {

        @Test
        void createDeletionCookie_validName_returnsCookieWithZeroMaxAge() {
            // Act
            ResponseCookie result = cookieService.createDeletionCookie("jwtToken");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("jwtToken");
            assertThat(result.getValue()).isEmpty();
            assertThat(result.getMaxAge().getSeconds()).isZero();
        }

        @Test
        void deleteCookieFromResponse_validInput_addsCookieHeaderToResponse() {
            // Act
            cookieService.deleteCookieFromResponse(response, "jwtToken");

            // Assert
            verify(response).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

            assertThat(headerNameCaptor.getValue()).isEqualTo(HttpHeaders.SET_COOKIE);
            assertThat(headerValueCaptor.getValue()).contains("jwtToken=");
            assertThat(headerValueCaptor.getValue()).contains("Max-Age=0");
        }
    }

    @Nested
    class GetCookieValueTests {

        @Test
        void getCookieValue_noCookiesInRequest_returnsEmptyOptional() {
            // Arrange
            when(request.getCookies()).thenReturn(null);

            // Act
            Optional<String> result = cookieService.getCookieValue(request, "jwtToken", String::valueOf);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getCookieValue_targetCookieMissing_returnsEmptyOptional() {
            // Arrange
            Cookie otherCookie = new Cookie("otherToken", "otherValue");
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

            // Act
            Optional<String> result = cookieService.getCookieValue(request, "jwtToken", String::valueOf);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getCookieValue_cookieExistsAndValid_returnsConvertedValue() {
            // Arrange
            Cookie targetCookie = new Cookie("userAge", "25");
            Cookie otherCookie = new Cookie("otherToken", "otherValue");
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie, targetCookie});

            // Act
            Optional<Integer> result = cookieService.getCookieValue(request, "userAge", Integer::valueOf);

            // Assert
            assertThat(result).isPresent().contains(25);
        }

        @Test
        void getCookieValue_conversionFails_returnsEmptyOptional() {
            // Arrange
            Cookie targetCookie = new Cookie("userAge", "invalid_number");
            when(request.getCookies()).thenReturn(new Cookie[]{targetCookie});

            // Act
            Optional<Integer> result = cookieService.getCookieValue(request, "userAge", Integer::valueOf);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}