package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final ApplicationConfigProperties.CookieConfig cookieConfig;

    public ResponseCookie createCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(cookieConfig.httpOnly())
                .secure(cookieConfig.secure())
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieConfig.sameSite());

        // prod
        // if (cookieConfig.domain() != null && !cookieConfig.domain().isBlank()) {
        //    builder.domain(cookieConfig.domain());
        // }

        return builder.build();
    }

    public void deleteCookieFromResponse(HttpServletResponse response, String cookieName) {
        ResponseCookie deletionCookie = createDeletionCookie(cookieName);
        response.addHeader(HttpHeaders.SET_COOKIE, deletionCookie.toString());
    }

    public ResponseCookie createDeletionCookie(String name) {
        return createCookie(name, "", 0);
    }

    public <T> Optional<T> getCookieValue(HttpServletRequest request, String cookieName, Function<String, T> converter) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .map(value -> {
                    try {
                        return converter.apply(value);
                    } catch (Exception e) {
                        return null; // Lub obsłuż błąd konwersji (np. logowanie)
                    }
                })
                .findFirst();
    }

}
