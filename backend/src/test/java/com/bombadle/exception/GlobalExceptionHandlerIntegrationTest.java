package com.bombadle.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerIntegrationTest {

    private MockMvc mockMvc;

    @RestController
    private static class DummyController {

        @GetMapping("/test/404")
        public void throwNotFound() {
            throw new UsernameNotFoundException("User missing");
        }

        @GetMapping("/test/500")
        public void throwGlobalException() throws Exception {
            throw new Exception("Database timeout");
        }

        @GetMapping("/test/409")
        public void throwConflict() {
            throw new RegistrationConflictException("Email exists");
        }

        @GetMapping("/test/401")
        public void throwUnauthorized() {
            throw new InvalidCredentialsException("Wrong password");
        }

        @GetMapping("/test/403")
        public void throwForbidden() {
            throw new AccessDeniedException("Forbidden action");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void whenUsernameNotFound_returns404AndJson() throws Exception {
        mockMvc.perform(get("/test/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.error").value("User Not Found"))
                .andExpect(jsonPath("$.message").value("User missing"));
    }

    @Test
    void whenGlobalException_returns500AndJson() throws Exception {
        mockMvc.perform(get("/test/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected errorDatabase timeout"));
    }

    @Test
    void whenRegistrationConflict_returns409AndJson() throws Exception {
        mockMvc.perform(get("/test/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.error").value("Registration conflict"))
                .andExpect(jsonPath("$.message").value("Email exists"));
    }

    @Test
    void whenInvalidCredentials_returns401AndJson() throws Exception {
        mockMvc.perform(get("/test/401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Wrong password"));
    }

    @Test
    void whenAccessDenied_returns403AndJson() throws Exception {
        mockMvc.perform(get("/test/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Forbidden action"));
    }
}