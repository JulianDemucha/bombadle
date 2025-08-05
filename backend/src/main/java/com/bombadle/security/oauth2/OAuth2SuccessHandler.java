package com.bombadle.security.oauth2;

import com.bombadle.entity.Player;
import com.bombadle.security.jwt.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2PlayerUser principal = (CustomOAuth2PlayerUser) authentication.getPrincipal();

        Player player = principal.getPlayer();

        String jwt = jwtService.generateToken(new HashMap<>(), player);
        response.setHeader("Authorization", "Bearer " + jwt);

        // response.sendRedirect("http://localhost:5173/oauth2/success?token=" + jwt);
    }
}
