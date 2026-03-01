package com.bombadle.service;

import com.bombadle.dto.RefreshTokenCookieDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final RefreshTokenService refreshTokenService;
    //todo create cookieconfig for maxage, httponly etc. from .env
    private int jwtMaxAge = 60*15;
    private int refreshTokenMaxAge = 60*60;
    private boolean httpOnly = true;
    private boolean secure = true;
    private String sameSite = "Lax";

    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite);

        // prod
        // if (cookieConfig.domain() != null && !cookieConfig.domain().isBlank()) {
        //    builder.domain(cookieConfig.domain());
        // }

        return builder.build();
    }

    public ResponseCookie createJwtCookie(String jwt) {
        return createCookie("jwt", jwt, jwtMaxAge);
    }

    public ResponseCookie createJwtTimerCookie() {
        long expiresAt = System.currentTimeMillis() + (jwtMaxAge*1000L);

        return ResponseCookie.from("JWT-EXPIRES-AT", String.valueOf(expiresAt))
                .httpOnly(false)
                .path("/")
                .maxAge(jwtMaxAge)
                .build();
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return createCookie("refreshToken", refreshToken, refreshTokenMaxAge);
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

    // USE ONLY WHEN REFRESHING REFRESHTOKEN - NOT WHEN refreshTokenCookieDto.jwt == null
    public void setAuthCookies(RefreshTokenCookieDto refreshTokenCookieDto, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                createJwtCookie(refreshTokenCookieDto.getJwt())
                        .toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                createJwtTimerCookie()
                        .toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshCookie(refreshTokenCookieDto.getRefreshToken())
                .toString());
    }

    public void clearCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("jwt", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refreshToken", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("JWT-EXPIRES-AT", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("XSRF-TOKEN", "", 0).toString());
    }


}
