package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

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

    public ResponseCookie createDeletionCookie(String name) {
        return createCookie(name, "", 0);
    }


}
