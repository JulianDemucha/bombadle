package com.bombadle.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;

@Component
public class CsrfCookieService {

    @Value("${app.security.csrf-cookie-max-age-seconds:3600}") //default 1h
    private long maxAgeSeconds;

    @Value("${app.security.secure-cookies:true}") //default true
    private boolean secureCookies;

    @Value("${app.security.csrf-cookie-same-site:Lax}") //default lax
    private String sameSite;

    public void ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        if (hasXsrfCookie(request)) return;
        addCsrfCookie(response);
    }

    private void addCsrfCookie(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", token)
                .httpOnly(false)
                .secure(secureCookies)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean hasXsrfCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        return Arrays.stream(cookies).anyMatch(c -> "XSRF-TOKEN".equals(c.getName()));
    }
}
