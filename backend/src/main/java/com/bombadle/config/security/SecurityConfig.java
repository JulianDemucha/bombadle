package com.bombadle.config.security;

import com.bombadle.security.filter.CsrfCookieInjectionFilter;
import com.bombadle.security.filter.StatelessCsrfValidationFilter;
import com.bombadle.security.filter.JwtAuthenticationFilter;
import com.bombadle.security.filter.AccountLockedFilter;
import com.bombadle.security.oauth2.CustomOAuth2UserService;
import com.bombadle.security.oauth2.OAuth2SuccessHandler;
import com.bombadle.service.auth.CsrfCookieService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final AccountLockedFilter accountLockedFilter;

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public CsrfCookieInjectionFilter csrfCookieFilter(CsrfCookieService csrfCookieService) {
        return new CsrfCookieInjectionFilter(csrfCookieService);
    }

    @Bean
    public StatelessCsrfValidationFilter statelessCsrfFilter() {
        return new StatelessCsrfValidationFilter(List.of(
                "/api/auth/authenticate",
                "/api/auth/register",
                "/api/auth/csrf"
        ));
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      CsrfCookieInjectionFilter csrfCookieFilter,
                                                      StatelessCsrfValidationFilter statelessCsrfFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/api/**", "swagger-ui", "/images/**", "/character_card_avatars/**")
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/daily-reset/manual-trigger").hasRole("SUPERADMIN")
                        .requestMatchers("/api/auth/check/**", "/api/auth/register", "/api/auth/authenticate", "/api/auth/refreshToken", "/api/card-guessing/classic/anonymous-guess/**", "/api/character-card/search-index", "/api/leaderboard/**", "/images/**", "/character_card_avatars/**" /*dev */).permitAll()
                        .anyRequest().authenticated()
                ).formLogin(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(customAuthenticationEntryPoint()).accessDeniedHandler(customAccessDeniedHandler()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(accountLockedFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, AccountLockedFilter.class)
                .addFilterAfter(statelessCsrfFilter, CsrfCookieInjectionFilter.class)
        ;


        return http.build();
    }

    @Bean
    @Order(2)   //prod: webSecurityFilterChain
    public SecurityFilterChain oauth2LoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/**", "/oauth2/**", "/error").permitAll()
                        .anyRequest().authenticated()
                ).formLogin(AbstractHttpConfigurer::disable)

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authz -> authz
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                        .userInfoEndpoint(u -> u
                                .oidcUserService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            exception.printStackTrace();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"" + exception.getMessage() + "\"}");
                        })
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // HttpSessionOAuth2AuthorizationRequestRepository
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
