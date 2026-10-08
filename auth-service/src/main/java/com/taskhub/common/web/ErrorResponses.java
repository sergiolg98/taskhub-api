package com.taskhub.common.web;

import com.taskhub.common.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static ResponseEntity<ErrorResponse> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message, details));
    }

    public static ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return build(status, message, List.of());
    }
}
