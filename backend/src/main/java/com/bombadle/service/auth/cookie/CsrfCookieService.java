package com.bombadle.service.auth.cookie;

import com.bombadle.config.ApplicationConfigProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CsrfCookieService {

    private final ApplicationConfigProperties.CsrfConfig csrfConfig;

    public void ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        if (hasXsrfCookie(request)) return;
        addCsrfCookie(response);
    }

    private void addCsrfCookie(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", token)
                .httpOnly(false)
                .secure(csrfConfig.secure())
                .path("/")
                .maxAge(csrfConfig.cookieMaxAgeSeconds())
                .sameSite(csrfConfig.sameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean hasXsrfCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        return Arrays.stream(cookies).anyMatch(c -> "XSRF-TOKEN".equals(c.getName()));
    }
}