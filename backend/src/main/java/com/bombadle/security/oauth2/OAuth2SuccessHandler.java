package com.bombadle.security.oauth2;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.entity.Player;
import com.bombadle.service.auth.AuthCookiesService;
import com.bombadle.service.auth.CsrfCookieService;
import com.bombadle.service.auth.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private static final String FRONTEND_SUCCESS_PATH = "/login-success";

    private final AuthCookiesService authCookiesService;
    private final RefreshTokenService refreshTokenService;
    private final CsrfCookieService csrfCookieService;
    private final ApplicationConfigProperties.FrontendConfig frontendConfig;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2PlayerUser principal = (CustomOAuth2PlayerUser) authentication.getPrincipal();
        Player player = principal.getPlayer();

        RefreshTokenCookieDto refreshData = refreshTokenService.createRefreshToken(player.getEmail());
        authCookiesService.setAuthCookies(refreshData.getJwt(), refreshData.getRefreshToken(), response);
        csrfCookieService.ensureCsrfCookie(request, response);

        String successUrl = resolveFrontendSuccessUrl();
        response.sendRedirect(successUrl);

    }

    private String resolveFrontendSuccessUrl() {
        String baseUrl = frontendConfig.baseUrl();

        String normalizedBaseUrl = baseUrl.trim().replaceAll("/+$", "");

        return normalizedBaseUrl + FRONTEND_SUCCESS_PATH;
    }
}
