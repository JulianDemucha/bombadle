package com.bombadle.config;

import com.bombadle.repository.PlayerRepository;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class ApplicationConfig {

    @Bean
    public UserDetailsService userDetailsService(PlayerRepository repo) {
        return email -> repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("user with email " + email + " not found"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ApplicationConfigProperties.JwtConfig jwtConfig(ApplicationConfigProperties config) {
        return config.jwt();
    }

    @Bean
    public ApplicationConfigProperties.CookieConfig cookieConfig(ApplicationConfigProperties config) {
        return config.cookie();
    }

    @Bean
    public ApplicationConfigProperties.FrontendConfig frontendConfig(ApplicationConfigProperties config) {
        return config.frontend();
    }

    @Bean
    public CurrentCharacterCardWrapper currentCharacterCard() {
        return new CurrentCharacterCardWrapper(null);
    }

    @Bean
    public CacheManager cacheManager(ApplicationConfigProperties config) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        var cacheConfig = config.cache();
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheConfig.defaultTtl())
        );
        cacheConfig.specs().forEach((cacheName, spec) -> {
            var ttl = spec.ttl() != null ? spec.ttl() : cacheConfig.defaultTtl();
            var builder = Caffeine.newBuilder().expireAfterWrite(ttl);
            if (spec.maxSize() != null) {
                builder.maximumSize(spec.maxSize());
            }
            cacheManager.registerCustomCache(cacheName, builder.build());
        });
        return cacheManager;

    }
}
