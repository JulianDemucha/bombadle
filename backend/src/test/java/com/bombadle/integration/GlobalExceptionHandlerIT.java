package com.bombadle.integration;

import com.bombadle.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

class GlobalExceptionHandlerIT {

    private MockMvc mockMvc;

    @RestController
    private static class DummyController {

        @GetMapping("/test/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid ID provided");
        }

        @GetMapping("/test/otp-not-found")
        public void throwOtpNotFound() {
            throw new OtpNotFoundException("OTP for this user not found");
        }

        @GetMapping("/test/expired-otp")
        public void throwExpiredOtp() {
            throw new ExpiredOtpException("This code has expired");
        }

        @GetMapping("/test/email-rate-limit")
        public void throwEmailRateLimit() {
            throw new EmailRateLimitException("You must wait 60 seconds before send", 60);
        }

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

        @GetMapping("/test/card-already-guessed")
        public void throwCardAlreadyGuessed() {
            throw new CardAlreadyGuessedException();
        }

        @GetMapping("/test/anonymous-session-already-guessed")
        public void throwAnonymousSessionAlreadyGuessed() {
            throw new AnonymousSessionAlreadyGuessedException();
        }

        @GetMapping("/test/character-card-not-found")
        public void throwCharacterCardNotFound() {
            throw new CharacterCardNotFoundException(99L);
        }

        @GetMapping("/test/unverified-email")
        public void throwUnverifiedEmail() {
            throw new UnverifiedEmailException("Account isn't verified", "test@test.com");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    class ClientErrorTests {

        @Test
        void whenIllegalArgument_returns400AndJson() throws Exception {
            mockMvc.perform(get("/test/illegal-argument"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Invalid ID provided"));
        }

        @Test
        void whenOtpNotFound_returns404AndJson() throws Exception {
            mockMvc.perform(get("/test/otp-not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.error").value("Verification code not found"))
                    .andExpect(jsonPath("$.message").value("OTP for this user not found"));
        }

        @Test
        void whenExpiredOtp_returns410AndJson() throws Exception {
            mockMvc.perform(get("/test/expired-otp"))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.statusCode").value(410))
                    .andExpect(jsonPath("$.error").value("Verification code has expired"))
                    .andExpect(jsonPath("$.message").value("This code has expired"));
        }

        @Test
        void whenEmailRateLimit_returns429AndJson() throws Exception {
            mockMvc.perform(get("/test/email-rate-limit"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.statusCode").value(429))
                    .andExpect(jsonPath("$.error").value("Too Many Requests"))
                    .andExpect(jsonPath("$.message").value("You must wait 60 seconds before send"));
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
        void whenCharacterCardNotFound_returns404AndJson() throws Exception {
            mockMvc.perform(get("/test/character-card-not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.error").value("Character Card Not Found"))
                    .andExpect(jsonPath("$.message").value("Character card with id 99 not found"));
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
        void whenCardAlreadyGuessed_returns409AndJson() throws Exception {
            mockMvc.perform(get("/test/card-already-guessed"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.error").value("Card Already Guessed"))
                    .andExpect(jsonPath("$.message").value("Card already guessed today"));
        }

        @Test
        void whenAnonymousSessionAlreadyGuessed_returns409AndJson() throws Exception {
            mockMvc.perform(get("/test/anonymous-session-already-guessed"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.error").value("Anonymous Session Already Guessed"))
                    .andExpect(jsonPath("$.message").value("This anonymous session has already been used to guess the card"));
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

        @Test
        void whenUnverifiedEmail_returns403AndJson() throws Exception {
            mockMvc.perform(get("/test/unverified-email"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.error").value("Unverified Email"))
                    .andExpect(jsonPath("$.message").value("Account isn't verified"))
                    .andExpect(jsonPath("$.email").value("test@test.com"));
        }
    }

    @Nested
    class ServerErrorTests {

        @Test
        void whenGlobalException_returns500AndJson() throws Exception {
            mockMvc.perform(get("/test/500"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("Unexpected errorDatabase timeout"));
        }
    }
}
