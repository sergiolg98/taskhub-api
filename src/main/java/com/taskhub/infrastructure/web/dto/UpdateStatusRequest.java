package com.taskhub.infrastructure.web.dto;

import com.taskhub.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TaskStatus status) {
}
