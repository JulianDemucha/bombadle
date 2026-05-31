package com.bombadle.security.oauth2;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.entity.Player;
import com.bombadle.service.auth.*;
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

    private final PostLoginService postLoginService;
    private final ApplicationConfigProperties.FrontendConfig frontendConfig;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        if (!(authentication.getPrincipal() instanceof CustomOAuth2PlayerUser principal)) {
            throw new IllegalStateException("Unexpected principal type: " + authentication.getPrincipal().getClass());
        }
        Player player = principal.getPlayer();

        postLoginService.processSuccessfulLogin(request, response, player);

        String successUrl = resolveFrontendSuccessUrl();
        response.sendRedirect(successUrl);

    }

    private String resolveFrontendSuccessUrl() {
        String baseUrl = frontendConfig.baseUrl();

        String normalizedBaseUrl = baseUrl.trim().replaceAll("/+$", "");

        return normalizedBaseUrl + FRONTEND_SUCCESS_PATH;
    }
}
