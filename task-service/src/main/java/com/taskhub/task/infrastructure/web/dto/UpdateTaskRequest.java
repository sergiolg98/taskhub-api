package com.taskhub.task.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description) {
}
