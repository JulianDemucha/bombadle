package com.bombadle.config.security;

import com.bombadle.controller.AuthenticationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthenticationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "bombadle.google.client-id=",
        "bombadle.google.client-secret=",
        "server.port=0"
})
class SecurityConfigGoogleDisabledTest extends AbstractSecurityConfigWebTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextStarts_withoutAnyClientRegistration() {
        assertThat(context.getBeansOfType(ClientRegistrationRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(2);
    }

    @Test
    void providers_isPublicAndReportsGoogleDisabled() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.google").value(false));
    }

    @Test
    void googleAuthorizationEndpoint_doesNotRedirectToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(header().doesNotExist("Location"));
    }
}
