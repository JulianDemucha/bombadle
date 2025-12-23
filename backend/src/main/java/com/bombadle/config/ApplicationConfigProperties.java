package com.bombadle.config;
import jakarta.servlet.http.Cookie;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bombadle")
public record ApplicationConfigProperties(
        JwtConfig jwt,
        CookieConfig cookie
) {
    public record JwtConfig(
            String secret,
            long expirationSeconds,
            long refreshExpirationSeconds
    ) {}

    public record CookieConfig(
            boolean secure,
            boolean httpOnly,
            String sameSite,
            String domain
    ) {}
}