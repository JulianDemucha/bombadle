package com.bombadle.security.oauth2;

import com.bombadle.entity.Player;
import com.bombadle.security.jwt.JwtService;
import com.bombadle.service.CsrfCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final CsrfCookieService csrfCookieService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2PlayerUser principal = (CustomOAuth2PlayerUser) authentication.getPrincipal();
        Player player = principal.getPlayer();

        String jwt = jwtService.generateToken(new HashMap<>(), player);
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60*60) //1h
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        csrfCookieService.ensureCsrfCookie(request, response);

        response.sendRedirect("http://localhost:5173/oauth2/success");

    }
}
