package com.taskhub.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(int status, String message, List<String> details, LocalDateTime timestamp) {

    public static ErrorResponse of(int status, String message, List<String> details) {
        return new ErrorResponse(status, message, details, LocalDateTime.now());
    }
}
