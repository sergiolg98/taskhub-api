package com.taskhub.task.infrastructure.web;

import com.taskhub.common.web.ErrorResponses;
import com.taskhub.common.web.dto.ErrorResponse;
import com.taskhub.task.domain.exception.ExternalServiceException;
import com.taskhub.task.domain.exception.InvalidTaskException;
import com.taskhub.task.domain.exception.OwnerNotFoundException;
import com.taskhub.task.domain.exception.TaskNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TaskExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskExceptionHandler.class);

    @ExceptionHandler({TaskNotFoundException.class, OwnerNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ErrorResponses.build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidTaskException ex) {
        return ErrorResponses.build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // The cause stays in the log (GlobalExceptionHandler's style); the client only learns that the check could not be made.
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternal(ExternalServiceException ex) {
        log.warn("External dependency failed: {}", ex.getMessage(), ex.getCause());
        return ErrorResponses.build(HttpStatus.SERVICE_UNAVAILABLE, "User verification is temporarily unavailable");
    }
}
