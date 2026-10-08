package com.taskhub.task.infrastructure.web.dto;

import com.taskhub.task.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TaskStatus status) {
}
