package com.taskhub.application.port.in;

public interface RegisterUserUseCase {

    /** Registers a new user (always with role USER) and returns an access token. */
    String register(String name, String email, String rawPassword);
}
