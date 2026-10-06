package com.taskhub.auth.application.port.out;

import com.taskhub.auth.domain.model.AuthenticatedUser;

public interface CredentialsAuthenticatorPort {

    /**
     * @throws com.taskhub.domain.exception.InvalidCredentialsException if the credentials are wrong
     */
    AuthenticatedUser authenticate(String email, String rawPassword);
}
