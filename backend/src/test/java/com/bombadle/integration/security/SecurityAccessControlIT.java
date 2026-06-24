package com.bombadle.integration.security;

import com.bombadle.controller.TestSecurityController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TestSecurityController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = OncePerRequestFilter.class
        )
)
@TestPropertySource(properties = {
        "test.security.controllers.enabled=true",
        "server.port=0"
})
class SecurityAccessControlIT {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(
                            (request, response, authException) -> response.setStatus(401)
                    ));
            return http.build();
        }
    }

    record SecurityCase(HttpMethod method, String url, String role, int expectedStatus) {}

    static Stream<SecurityCase> matrix() {
        return Stream.of(
                new SecurityCase(HttpMethod.GET, "/test/security/admin", "ROLE_ADMIN", 200),
                new SecurityCase(HttpMethod.GET, "/test/security/admin", "ROLE_SUPERADMIN", 200),
                new SecurityCase(HttpMethod.GET, "/test/security/admin", "ROLE_USER", 403),
                new SecurityCase(HttpMethod.GET, "/test/security/admin", null, 401),

                new SecurityCase(HttpMethod.GET, "/test/security/superadmin", "ROLE_SUPERADMIN", 200),
                new SecurityCase(HttpMethod.GET, "/test/security/superadmin", "ROLE_ADMIN", 403)
        );
    }

    @ParameterizedTest(name = "[{index}] Metoda {0} na {1} z rolą {2} powinna zwrócić {3}")
    @MethodSource("matrix")
    void security_matrix(SecurityCase tc) throws Exception {

        var requestBuilder = request(tc.method, tc.url);

        if (tc.role != null) {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "TestUser", null, List.of(new SimpleGrantedAuthority(tc.role))
            );
            requestBuilder = requestBuilder.with(authentication(auth));
        }

        mockMvc.perform(requestBuilder).andExpect(status().is(tc.expectedStatus));
    }
}