package com.taskhub.task.domain.model;

import com.taskhub.task.domain.exception.InvalidTaskException;

import java.time.LocalDateTime;

public class Task {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private final Long ownerId;
    // false when the owner could not be checked against auth-service at creation time (it was unavailable).
    private final boolean ownerVerified;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(Long id, String title, String description, TaskStatus status, Long ownerId,
                boolean ownerVerified, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("Task title must not be blank");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.ownerId = ownerId;
        this.ownerVerified = ownerVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Task create(String title, String description, Long ownerId) {
        return create(title, description, ownerId, true);
    }

    public static Task create(String title, String description, Long ownerId, boolean ownerVerified) {
        LocalDateTime now = LocalDateTime.now();
        return new Task(null, title, description, TaskStatus.PENDING, ownerId, ownerVerified, now, now);
    }

    public void update(String title, String description) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("Task title must not be blank");
        }
        this.title = title;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(TaskStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public Long getOwnerId() { return ownerId; }
    public boolean isOwnerVerified() { return ownerVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
