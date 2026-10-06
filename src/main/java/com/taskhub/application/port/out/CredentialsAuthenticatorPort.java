package com.taskhub.application.port.out;

import com.taskhub.domain.model.AuthenticatedUser;

public interface CredentialsAuthenticatorPort {

    /**
     * @throws com.taskhub.domain.exception.InvalidCredentialsException if the credentials are wrong
     */
    AuthenticatedUser authenticate(String email, String rawPassword);
}
