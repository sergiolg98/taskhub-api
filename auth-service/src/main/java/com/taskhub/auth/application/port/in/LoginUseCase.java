package com.taskhub.auth.application.port.in;

public interface LoginUseCase {

    /** Verifies the credentials and returns an access token. */
    String login(String email, String rawPassword);
}
