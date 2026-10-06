package com.taskhub.application.service;

import com.taskhub.application.port.in.LoginUseCase;
import com.taskhub.application.port.in.RegisterUserUseCase;
import com.taskhub.application.port.out.CredentialsAuthenticatorPort;
import com.taskhub.application.port.out.PasswordHasherPort;
import com.taskhub.application.port.out.TokenIssuerPort;
import com.taskhub.application.port.out.UserRepositoryPort;
import com.taskhub.domain.exception.EmailAlreadyUsedException;
import com.taskhub.domain.model.AuthenticatedUser;
import com.taskhub.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements RegisterUserUseCase, LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final CredentialsAuthenticatorPort authenticator;
    private final TokenIssuerPort tokenIssuer;

    public AuthService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                       CredentialsAuthenticatorPort authenticator, TokenIssuerPort tokenIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.authenticator = authenticator;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional
    public String register(String name, String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User saved = users.save(User.register(name, email, passwordHasher.hash(rawPassword)));
        return tokenIssuer.issue(saved.getEmail(), saved.getRole());
    }

    @Override
    public String login(String email, String rawPassword) {
        AuthenticatedUser user = authenticator.authenticate(email, rawPassword);
        return tokenIssuer.issue(email, user.role());
    }
}
