package com.taskhub.auth.infrastructure.web;

import com.taskhub.auth.domain.exception.EmailAlreadyUsedException;
import com.taskhub.auth.domain.exception.InvalidCredentialsException;
import com.taskhub.auth.domain.exception.UserNotFoundException;
import com.taskhub.common.web.ErrorResponses;
import com.taskhub.common.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        return ErrorResponses.build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EmailAlreadyUsedException ex) {
        return ErrorResponses.build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ErrorResponses.build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
