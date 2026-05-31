package com.bombadle.exception;

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

    @Test
    void handleUsernameNotFound_ShouldReturn404AndCorrectBody() {
        UsernameNotFoundException exception = new UsernameNotFoundException("User missing");
        ResponseEntity<ErrorResponse> response = handler.handleUsernameNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("User Not Found", response.getBody().error());
        assertEquals("User missing", response.getBody().message());
    }

    @Test
    void handleGlobalException_ShouldReturn500AndCorrectBody() {
        Exception exception = new Exception("Database timeout");
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().statusCode());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("Unexpected errorDatabase timeout", response.getBody().message());
    }

    @Test
    void handleUsernameAlreadyTaken_ShouldReturn409AndCorrectBody() {
        UsernameAlreadyTakenException exception = new UsernameAlreadyTakenException("Name taken");
        ResponseEntity<ErrorResponse> response = handler.handleUsernameAlreadyTaken(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().statusCode());
        assertEquals("Username already exists in the database", response.getBody().error());
        assertEquals("Name taken", response.getBody().message());
    }

    @Test
    void handleAuthenticationMissing_ShouldReturn401AndCorrectBody() {
        AuthenticationCredentialsNotFoundException exception = new AuthenticationCredentialsNotFoundException("No token");
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationMissing(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("No token", response.getBody().message());
    }

    @Test
    void handleScoreNotFound_ShouldReturn404AndCorrectBody() {
        ScoreNotFoundException exception = new ScoreNotFoundException("Score 123 missing");
        ResponseEntity<ErrorResponse> response = handler.handleScoreNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("Score Not Found", response.getBody().error());
        assertEquals("Score 123 missing", response.getBody().message());
    }

    @Test
    void handleInvalidCredentials_ShouldReturn401AndCorrectBody() {
        InvalidCredentialsException exception = new InvalidCredentialsException("Wrong password");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Wrong password", response.getBody().message());
    }

    @Test
    void handleRegistrationConflict_ShouldReturn409AndCorrectBody() {
        RegistrationConflictException exception = new RegistrationConflictException("Email exists");
        ResponseEntity<ErrorResponse> response = handler.handleRegistrationConflict(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().statusCode());
        assertEquals("Registration conflict", response.getBody().error());
        assertEquals("Email exists", response.getBody().message());
    }

    @Test
    void handleRegistrationValidation_ShouldReturn409AndCorrectBody() {
        RegistrationValidationException exception = new RegistrationValidationException("Password too short");
        ResponseEntity<ErrorResponse> response = handler.handleRegistrationValidation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().statusCode());
        assertEquals("Registration validation failed", response.getBody().error());
        assertEquals("Password too short", response.getBody().message());
    }

    @Test
    void handleAdminOperationNotAllowed_ShouldReturn409AndCorrectBody() {
        AdminOperationNotAllowedException exception = new AdminOperationNotAllowedException("Action blocked");
        ResponseEntity<ErrorResponse> response = handler.handleAdminOperationNotAllowed(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().statusCode());
        assertEquals("Admin operation not allowed", response.getBody().error());
        assertEquals("Action blocked", response.getBody().message());
    }

    @Test
    void handleAccessDenied_ShouldReturn403AndCorrectBody() {
        AccessDeniedException exception = new AccessDeniedException("Forbidden action");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(exception);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().statusCode());
        assertEquals("Forbidden", response.getBody().error());
        assertEquals("Forbidden action", response.getBody().message());
    }
}