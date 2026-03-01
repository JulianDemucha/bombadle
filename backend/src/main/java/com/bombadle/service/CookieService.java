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
    //todo zrobic config i z niego pobierac maxage
    private int jwtMaxAge = 60*15;
    private int refreshTokenMaxAge = 60*60;

    public ResponseCookie createJwtCookie(String jwt) {
        return ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtMaxAge)
                .sameSite("Lax")
                .build();
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
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenMaxAge)
                .sameSite("Strict")
                .build();
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


}
