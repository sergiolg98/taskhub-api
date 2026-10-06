package com.taskhub.task.domain.exception;

public class InvalidTaskException extends RuntimeException {

    public InvalidTaskException(String message) {
        super(message);
    }
}
