package com.bombadle.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {

        ErrorResponse errorDetails = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "User Not Found",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails); //404
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {

        ErrorResponse errorDetails = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                "Internal Server Error",
                "Unexpected error" + ex.getMessage()
        );

        log.error("Unexpected error: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails); //500
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        ErrorResponse errorDetails = new ErrorResponse(
                HttpStatus.CONFLICT.value(), //409
                "Username already exists in the database",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationMissing(AuthenticationCredentialsNotFoundException ex) {
        ErrorResponse errorDetails = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), // 401
                "Unauthorized",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }

}
