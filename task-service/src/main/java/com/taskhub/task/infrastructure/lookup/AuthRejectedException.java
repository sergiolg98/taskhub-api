package com.taskhub.task.infrastructure.lookup;

/**
 * auth-service answered a 4xx other than 404 (e.g. 401 or 403 for the forwarded token). It is alive, so it must not open the
 * circuit nor be retried: asking again with the same token gets the same answer.
 */
public class AuthRejectedException extends RuntimeException {

    public AuthRejectedException(int status) {
        super("auth-service rejected the request with " + status);
    }
}
