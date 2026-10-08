package com.taskhub.task.domain.exception;

/** A service this one depends on did not answer properly (down, slow, or answering errors). It says nothing about the data. */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
