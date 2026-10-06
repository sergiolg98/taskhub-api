package com.taskhub.common.web;

import com.taskhub.common.web.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

// Only generic errors live here. Each business area maps its own exceptions.
// Every response, including the ones Spring generates (405, 415, unknown route, 500), uses the same ErrorResponse format.
// Lowest precedence: advices are consulted in order and the first one with a matching handler wins.
// The safety net for Exception must come after the handlers of each business area, or it would shadow them.
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ErrorResponses.build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ErrorResponses.build(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ErrorResponses.build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }

    // Safety net. IllegalArgumentException is deliberately NOT mapped to 400: it also signals programming errors
    // (an earlier version hid a BCrypt failure behind a vague 400).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) throws Exception {
        // Security exceptions belong to Spring Security: it answers 401/403 itself.
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        // Framework exceptions that already know their status: 405, 415, unknown route...
        if (ex instanceof org.springframework.web.ErrorResponse framework) {
            HttpStatus status = HttpStatus.valueOf(framework.getStatusCode().value());
            return ErrorResponses.build(status, status.getReasonPhrase());
        }
        log.error("Unexpected error", ex);
        return ErrorResponses.build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
