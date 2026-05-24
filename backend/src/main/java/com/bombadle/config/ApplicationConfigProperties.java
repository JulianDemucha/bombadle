package com.bombadle.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "bombadle")
public record ApplicationConfigProperties(
        JwtConfig jwt,
        CookieConfig cookie,
        CsrfConfig csrf,
        FrontendConfig frontend,
        CacheConfig cache
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

    public record FrontendConfig(
            String baseUrl
    ) {}

    public record CacheConfig(
            Duration defaultTtl,
            Map<String, CacheSpec> specs
    ){}

    public record CacheSpec(
            Duration ttl,
            Long maxSize
    ){}
}