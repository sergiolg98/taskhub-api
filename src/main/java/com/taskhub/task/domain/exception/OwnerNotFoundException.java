package com.taskhub.task.domain.exception;

public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException(Long ownerId) {
        super("Owner not found: " + ownerId);
    }
}
