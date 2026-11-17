package com.bombadle.config.security;

import com.bombadle.security.filter.AccountLockedFilter;
import com.bombadle.security.filter.ActivityTrackingFilter;
import com.bombadle.security.filter.JwtAuthenticationFilter;
import com.bombadle.security.oauth2.CustomOAuth2UserService;
import com.bombadle.security.oauth2.OAuth2SuccessHandler;
import com.bombadle.service.auth.AuthenticationService;
import com.bombadle.service.auth.PostLoginService;
import com.bombadle.service.auth.cookie.AuthCookiesService;
import com.bombadle.service.auth.cookie.CsrfCookieService;
import com.bombadle.service.auth.cookie.RefreshTokenService;
import com.bombadle.service.auth.email.EmailActionInitiator;
import com.bombadle.service.auth.email.EmailConfirmationService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Loads the real SecurityConfig with its collaborators mocked; mocked filters just pass the request on. */
abstract class AbstractSecurityConfigWebTest {

    @MockitoBean JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean AccountLockedFilter accountLockedFilter;
    @MockitoBean ActivityTrackingFilter activityTrackingFilter;
    @MockitoBean AuthenticationProvider authenticationProvider;
    @MockitoBean CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean CsrfCookieService csrfCookieService;

    @MockitoBean AuthenticationService authenticationService;
    @MockitoBean RefreshTokenService refreshTokenService;
    @MockitoBean AuthCookiesService authCookiesService;
    @MockitoBean PostLoginService postLoginService;
    @MockitoBean EmailConfirmationService emailConfirmationService;
    @MockitoBean EmailActionInitiator emailActionInitiator;

    @BeforeEach
    void mockedFiltersPassRequestOn() throws Exception {
        for (Filter filter : List.of(jwtAuthFilter, accountLockedFilter, activityTrackingFilter)) {
            doAnswer(invocation -> {
                ServletRequest request = invocation.getArgument(0);
                ServletResponse response = invocation.getArgument(1);
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(request, response);
                return null;
            }).when(filter).doFilter(any(), any(), any());
        }
    }
}
