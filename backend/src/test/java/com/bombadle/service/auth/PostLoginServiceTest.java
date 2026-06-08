package com.bombadle.service.auth;

import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.entity.Player;
import com.bombadle.service.auth.cookie.AuthCookiesService;
import com.bombadle.service.auth.cookie.CookieService;
import com.bombadle.service.auth.cookie.CsrfCookieService;
import com.bombadle.service.auth.cookie.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLoginServiceTest {

    @InjectMocks
    private PostLoginService postLoginService;

    @Mock
    private AuthCookiesService authCookiesService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CsrfCookieService csrfCookieService;

    @Mock
    private AnonymousMergeService anonymousMergeService;

    @Mock
    private CookieService cookieService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Player player;

    private final String TEST_EMAIL = "player@example.com";
    private final String JWT_TOKEN = "jwt.token.value";
    private final String REFRESH_TOKEN = "refresh.token.value";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        player = Player.builder().build();
        player.setEmail(TEST_EMAIL);
    }

    @Test
    void processSuccessfulLogin_withAnonSessionAndMergeTrigger_executesFullFlowAndDeletesCookies() {
        // Arrange
        player.setEmailVerified(true);
        UUID anonSessionId = UUID.randomUUID();

        when(cookieService.getCookieValue(eq(request), eq("ANON_SESSION_ID"), any()))
                .thenReturn(Optional.of(anonSessionId));
        when(cookieService.getCookieValue(eq(request), eq("TRIGGER_MERGE"), any()))
                .thenReturn(Optional.of(true));

        RefreshTokenCookieDto tokenDto = RefreshTokenCookieDto.builder().jwt(JWT_TOKEN).refreshToken(REFRESH_TOKEN).build();
        when(refreshTokenService.createRefreshToken(TEST_EMAIL)).thenReturn(tokenDto);

        // Act
        postLoginService.processSuccessfulLogin(request, response, player);

        // Assert
        verify(anonymousMergeService).handleAnonymousSessionMerge(player, anonSessionId, true);
        verify(refreshTokenService).createRefreshToken(TEST_EMAIL);
        verify(authCookiesService).setAuthCookies(JWT_TOKEN, REFRESH_TOKEN, response);
        verify(csrfCookieService).ensureCsrfCookie(request, response);

        verify(cookieService).deleteCookieFromResponse(response, "ANON_SESSION_ID");
        verify(cookieService).deleteCookieFromResponse(response, "TRIGGER_MERGE");
    }

    @Test
    void processSuccessfulLogin_withoutCookies_executesFlowWithoutMergeAndCookieDeletion() {
        // Arrange
        player.setEmailVerified(true);
        when(cookieService.getCookieValue(eq(request), eq("ANON_SESSION_ID"), any()))
                .thenReturn(Optional.empty());
        when(cookieService.getCookieValue(eq(request), eq("TRIGGER_MERGE"), any()))
                .thenReturn(Optional.empty());

        RefreshTokenCookieDto tokenDto = RefreshTokenCookieDto.builder().jwt(JWT_TOKEN).refreshToken(REFRESH_TOKEN).build();
        when(refreshTokenService.createRefreshToken(TEST_EMAIL)).thenReturn(tokenDto);

        // Act
        postLoginService.processSuccessfulLogin(request, response, player);

        // Assert
        verify(anonymousMergeService).handleAnonymousSessionMerge(player, null, false);

        verify(refreshTokenService).createRefreshToken(TEST_EMAIL);
        verify(authCookiesService).setAuthCookies(JWT_TOKEN, REFRESH_TOKEN, response);
        verify(csrfCookieService).ensureCsrfCookie(request, response);

        verify(cookieService, never()).deleteCookieFromResponse(response, "ANON_SESSION_ID");
        verify(cookieService, never()).deleteCookieFromResponse(response, "TRIGGER_MERGE");
    }

    @Test
    void processSuccessfulLogin_onlyAnonSessionIdPresent_deletesOnlyAnonCookie() {
        // Arrange
        player.setEmailVerified(true);
        UUID anonSessionId = UUID.randomUUID();

        when(cookieService.getCookieValue(eq(request), eq("ANON_SESSION_ID"), any()))
                .thenReturn(Optional.of(anonSessionId));
        when(cookieService.getCookieValue(eq(request), eq("TRIGGER_MERGE"), any()))
                .thenReturn(Optional.empty());

        RefreshTokenCookieDto tokenDto = RefreshTokenCookieDto.builder().jwt(JWT_TOKEN).refreshToken(REFRESH_TOKEN).build();
        when(refreshTokenService.createRefreshToken(TEST_EMAIL)).thenReturn(tokenDto);

        // Act
        postLoginService.processSuccessfulLogin(request, response, player);

        // Assert
        verify(anonymousMergeService).handleAnonymousSessionMerge(player, anonSessionId, false);
        verify(cookieService).deleteCookieFromResponse(response, "ANON_SESSION_ID");
        verify(cookieService, never()).deleteCookieFromResponse(response, "TRIGGER_MERGE");
    }

    @Test
    void processSuccessfulLogin_emailNotVerified_doesNotCreateAuthTokens() {
        // Arrange
        player.setEmailVerified(false);
        when(cookieService.getCookieValue(eq(request), eq("ANON_SESSION_ID"), any()))
                .thenReturn(Optional.empty());
        when(cookieService.getCookieValue(eq(request), eq("TRIGGER_MERGE"), any()))
                .thenReturn(Optional.empty());

        // Act
        postLoginService.processSuccessfulLogin(request, response, player);

        // Assert
        verify(anonymousMergeService).handleAnonymousSessionMerge(player, null, false);
        verify(refreshTokenService, never()).createRefreshToken(any());
        verify(authCookiesService, never()).setAuthCookies(any(), any(), any());
        verify(csrfCookieService).ensureCsrfCookie(request, response);
    }
}