package com.bombadle.exception;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    class BadRequestExceptions {

        @Test
        void handleIllegalArgument_returns400AndCorrectBody() {
            // Arrange
            IllegalArgumentException exception = new IllegalArgumentException("Invalid name");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().statusCode());
            assertEquals("Bad Request", response.getBody().error());
            assertEquals("Invalid name", response.getBody().message());
        }
    }

    @Nested
    class NotFoundExceptions {

        @Test
        void handleUsernameNotFound_returns404AndCorrectBody() {
            // Arrange
            UsernameNotFoundException exception = new UsernameNotFoundException("User missing");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleUsernameNotFound(exception);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().statusCode());
            assertEquals("User Not Found", response.getBody().error());
            assertEquals("User missing", response.getBody().message());
        }

        @Test
        void handleScoreNotFound_returns404AndCorrectBody() {
            // Arrange
            ScoreNotFoundException exception = new ScoreNotFoundException("Score 123 missing");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleScoreNotFound(exception);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().statusCode());
            assertEquals("Score Not Found", response.getBody().error());
            assertEquals("Score 123 missing", response.getBody().message());
        }

        @Test
        void handleCharacterCardNotFound_returns404AndCorrectBody() {
            // Arrange
            CharacterCardNotFoundException exception = new CharacterCardNotFoundException(99L);

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleCharacterCardNotFound(exception);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().statusCode());
            assertEquals("Character Card Not Found", response.getBody().error());
            assertEquals("Character card with id 99 not found", response.getBody().message());
        }

        @Test
        void handleOtpNotFound_returns404AndCorrectBody() {
            // Arrange
            OtpNotFoundException exception = new OtpNotFoundException("OTP not found");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleOtpNotFound(exception);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().statusCode());
            assertEquals("Verification code not found", response.getBody().error());
            assertEquals("OTP not found", response.getBody().message());
        }
    }

    @Nested
    class ConflictExceptions {

        @Test
        void handleUsernameAlreadyTaken_returns409AndCorrectBody() {
            // Arrange
            UsernameAlreadyTakenException exception = new UsernameAlreadyTakenException("Name taken");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleUsernameAlreadyTaken(exception);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().statusCode());
            assertEquals("Username already exists in the database", response.getBody().error());
            assertEquals("Name taken", response.getBody().message());
        }

        @Test
        void handleRegistrationConflict_returns409AndCorrectBody() {
            // Arrange
            RegistrationConflictException exception = new RegistrationConflictException("Email exists");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleRegistrationConflict(exception);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().statusCode());
            assertEquals("Registration conflict", response.getBody().error());
            assertEquals("Email exists", response.getBody().message());
        }

        @Test
        void handleRegistrationValidation_returns409AndCorrectBody() {
            // Arrange
            RegistrationValidationException exception = new RegistrationValidationException("Password too short");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleRegistrationValidation(exception);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().statusCode());
            assertEquals("Registration validation failed", response.getBody().error());
            assertEquals("Password too short", response.getBody().message());
        }

        @Test
        void handleAdminOperationNotAllowed_returns409AndCorrectBody() {
            // Arrange
            AdminOperationNotAllowedException exception = new AdminOperationNotAllowedException("Action blocked");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleAdminOperationNotAllowed(exception);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().statusCode());
            assertEquals("Admin operation not allowed", response.getBody().error());
            assertEquals("Action blocked", response.getBody().message());
        }

        @Test
        void handleCardAlreadyGuessed_returns409AndCorrectBody() {
            // Arrange
            CardAlreadyGuessedException exception = new CardAlreadyGuessedException();

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleCardAlreadyGuessed(exception);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().statusCode());
            assertEquals("Card Already Guessed", response.getBody().error());
            assertEquals("Card already guessed today", response.getBody().message());
        }

        @Test
        void handleAnonymousSessionAlreadyGuessed_returns409AndCorrectBody() {
            // Arrange
            AnonymousSessionAlreadyGuessedException exception = new AnonymousSessionAlreadyGuessedException();

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleAnonymousSessionAlreadyGuessed(exception);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().statusCode());
            assertEquals("Anonymous Session Already Guessed", response.getBody().error());
            assertEquals("This anonymous session has already been used to guess the card", response.getBody().message());
        }
    }

    @Nested
    class GoneExceptions {

        @Test
        void handleExpiredOtp_returns410AndCorrectBody() {
            // Arrange
            ExpiredOtpException exception = new ExpiredOtpException("Code expired");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleExpiredOtp(exception);

            // Assert
            assertEquals(HttpStatus.GONE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(410, response.getBody().statusCode());
            assertEquals("Verification code has expired", response.getBody().error());
            assertEquals("Code expired", response.getBody().message());
        }
    }

    @Nested
    class TooManyRequestsExceptions {

        @Test
        void handleEmailRateLimit_returns429AndCorrectBody() {
            // Arrange
            EmailRateLimitException exception = new EmailRateLimitException("You must wait 60 seconds before send");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleEmailRateLimit(exception);

            // Assert
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(429, response.getBody().statusCode());
            assertEquals("Too Many Requests", response.getBody().error());
            assertEquals("You must wait 60 seconds before send", response.getBody().message());
        }
    }

    @Nested
    class SecurityExceptions {

        @Test
        void handleAuthenticationMissing_returns401AndCorrectBody() {
            // Arrange
            AuthenticationCredentialsNotFoundException exception = new AuthenticationCredentialsNotFoundException("No token");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleAuthenticationMissing(exception);

            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(401, response.getBody().statusCode());
            assertEquals("Unauthorized", response.getBody().error());
            assertEquals("No token", response.getBody().message());
        }

        @Test
        void handleInvalidCredentials_returns401AndCorrectBody() {
            // Arrange
            InvalidCredentialsException exception = new InvalidCredentialsException("Wrong password");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(exception);

            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(401, response.getBody().statusCode());
            assertEquals("Unauthorized", response.getBody().error());
            assertEquals("Wrong password", response.getBody().message());
        }

        @Test
        void handleAccessDenied_returns403AndCorrectBody() {
            // Arrange
            AccessDeniedException exception = new AccessDeniedException("Forbidden action");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(exception);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(403, response.getBody().statusCode());
            assertEquals("Forbidden", response.getBody().error());
            assertEquals("Forbidden action", response.getBody().message());
        }

        @Test
        void handleUnverifiedEmail_returns403AndCorrectBody() {
            // Arrange
            UnverifiedEmailException exception = new UnverifiedEmailException("Account isn't verified", "test@test.com");

            // Act
            ResponseEntity<ErrorResponseWithEmail> response = handler.handleUnverifiedEmail(exception);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(403, response.getBody().statusCode());
            assertEquals("Unverified Email", response.getBody().error());
            assertEquals("Account isn't verified", response.getBody().message());
            assertEquals("test@test.com", response.getBody().email());
        }

        @Test
        void handleInvalidOtp_returns403AndCorrectBody() {
            // Arrange
            InvalidOtpException exception = new InvalidOtpException("Wrong code");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleInvalidOtp(exception);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(403, response.getBody().statusCode());
            assertEquals("Invalid verification code", response.getBody().error());
            assertEquals("Wrong code", response.getBody().message());
        }
    }

    @Nested
    class ServerExceptions {

        @Test
        void handleGlobalException_returns500AndCorrectBody() {
            // Arrange
            Exception exception = new Exception("Database timeout");

            // Act
            ResponseEntity<ErrorResponse> response = handler.handleGlobalException(exception);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().statusCode());
            assertEquals("Internal Server Error", response.getBody().error());
            assertEquals("Unexpected errorDatabase timeout", response.getBody().message());
        }
    }
}
