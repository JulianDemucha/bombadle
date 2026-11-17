package com.bombadle.config.security;

import com.bombadle.config.ApplicationConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/** Registers Google as the OAuth2 provider only when credentials are configured. */
@Configuration
public class GoogleOAuthConfig {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @Bean
    @Conditional(GoogleLoginEnabledCondition.class)
    public ClientRegistrationRepository clientRegistrationRepository(ApplicationConfigProperties config) {
        ApplicationConfigProperties.GoogleConfig google = config.google();

        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(GOOGLE_ISSUER)
                .registrationId("google")
                .clientId(google.clientId())
                .clientSecret(google.clientSecret())
                .scope("openid", "profile", "email")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
