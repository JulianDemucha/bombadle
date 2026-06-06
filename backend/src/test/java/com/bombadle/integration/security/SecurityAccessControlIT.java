package com.bombadle.integration.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessControlIT {

    @Autowired
    private MockMvc mockMvc;

    private final String csrfToken = "sigma-csrf-token";
    private final Cookie csrfCookie = new Cookie("XSRF-TOKEN", csrfToken);

    // Unauthenticated access checks
    @Test
    void protectedEndpoint_withoutToken_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/players/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoint_withoutToken_shouldNotReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/check/email").param("email", "bomba@gwiezdnaflota.com"))
                .andExpect(status().isOk());
    }

    // Role-based access control
    @Test
    @WithMockPlayer(username = "glus@sigma.com")
    void adminEndpoint_withUserRole_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/character-cards"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPlayer(username = "admiral@sigma.com", role = "ROLE_ADMIN")
    void adminEndpoint_withAdminRole_shouldNotReturn403() throws Exception {
        // Security passes, but endpoint is missing/unhandled, resulting in 500
        mockMvc.perform(get("/api/admin/character-cards"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockPlayer(username = "glus@sigma.com")
    void superAdminEndpoint_withUserRole_shouldReturn403Forbidden() throws Exception {
        // CSRF injected to ensure the 403 comes from RBAC, not from missing CSRF token
        mockMvc.perform(post("/api/daily-reset/manual-trigger")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isForbidden());
    }

    // AccountLockedFilter protection
    @Test
    @WithMockPlayer(username = "sigma@sigma.com", accountLocked = true)
    void protectedEndpoint_whenAccountIsLocked_shouldReturn423Locked() throws Exception {
        mockMvc.perform(post("/api/daily-reset/manual-trigger"))
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.error").value("Account Locked"))
                .andExpect(jsonPath("$.message").value("Account is locked"));
    }

    @Test
    @WithMockPlayer(username = "sigma@sigma.com", accountLocked = true)
    void excludedEndpoint_whenAccountIsLocked_shouldBypassFilter() throws Exception {
        // Security bypasses the filter, controller handles the request and throws 404 (DB is empty)
        mockMvc.perform(get("/api/players/me"))
                .andExpect(status().isNotFound());
    }
}