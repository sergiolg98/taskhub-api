package com.taskhub.task.infrastructure.web.dto;

import com.taskhub.task.domain.model.Task;
import com.taskhub.task.domain.model.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Long ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TaskResponse from(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getOwnerId(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
