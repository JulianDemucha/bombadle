package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthCookiesService {
    private final ApplicationConfigProperties.JwtConfig jwtConfig;
    private final CookieService cookieService;
    private final String[] cookieNames = {"jwt", "refreshToken", "JWT-EXPIRES-AT", "XSRF-TOKEN"};

    public ResponseCookie createJwtCookie(String jwt) {
        Objects.requireNonNull(jwt, "JWT token must not be null when creating a cookie");
        return cookieService.createCookie("jwt", jwt, jwtConfig.expirationSeconds());
    }

    public ResponseCookie createJwtTimerCookie() {
        long expiresAt = System.currentTimeMillis() + (jwtConfig.expirationSeconds()*1000L);

        return ResponseCookie.from("JWT-EXPIRES-AT", String.valueOf(expiresAt))
                .httpOnly(false)
                .path("/")
                .maxAge(jwtConfig.expirationSeconds())
                .build();
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        Objects.requireNonNull(refreshToken, "Refresh token must not be null when creating a cookie");
        return cookieService.createCookie("refreshToken", refreshToken, jwtConfig.refreshExpirationSeconds());
    }

    public void setAuthCookies(String jwt, String refreshToken, HttpServletResponse response) {

        response.addHeader(HttpHeaders.SET_COOKIE,
                createJwtCookie(jwt)
                        .toString());

        response.addHeader(HttpHeaders.SET_COOKIE,
                createJwtTimerCookie()
                        .toString());

        response.addHeader(HttpHeaders.SET_COOKIE,
                createRefreshCookie(refreshToken)
                        .toString());

    }

    public HttpHeaders createClearCookiesHeaders() {
        HttpHeaders headers = new HttpHeaders();
        for(String cookieName : cookieNames) {
            headers.add(HttpHeaders.SET_COOKIE, cookieService.createDeletionCookie(cookieName).toString());
        }
        return headers;
    }


}
