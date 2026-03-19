package com.bombadle.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bombadle")
public record ApplicationConfigProperties(
        JwtConfig jwt,
        CookieConfig cookie,
        CsrfConfig csrf
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

    public record CsrfConfig(
            long cookieMaxAgeSeconds,
            boolean secure,
            String sameSite
    ) {}
}