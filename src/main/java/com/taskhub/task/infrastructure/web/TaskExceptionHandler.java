package com.taskhub.task.infrastructure.web;

import com.taskhub.common.web.ErrorResponses;
import com.taskhub.common.web.dto.ErrorResponse;
import com.taskhub.task.domain.exception.OwnerNotFoundException;
import com.taskhub.task.domain.exception.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TaskExceptionHandler {

    @ExceptionHandler({TaskNotFoundException.class, OwnerNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ErrorResponses.build(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
