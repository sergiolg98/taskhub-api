package com.taskhub.task.infrastructure.lookup;

/**
 * auth-service failed in a way that says nothing about the user: 5xx, connection refused, timeout, broken connection.
 * It is the ONLY exception the circuit breaker counts as a failure and the retry repeats.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
