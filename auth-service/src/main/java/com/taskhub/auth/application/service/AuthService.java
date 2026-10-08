package com.taskhub.auth.application.service;

import com.taskhub.auth.application.port.in.LoginUseCase;
import com.taskhub.auth.application.port.in.RegisterUserUseCase;
import com.taskhub.auth.application.port.out.CredentialsAuthenticatorPort;
import com.taskhub.auth.application.port.out.PasswordHasherPort;
import com.taskhub.auth.application.port.out.TokenIssuerPort;
import com.taskhub.auth.application.port.out.UserRepositoryPort;
import com.taskhub.auth.domain.exception.EmailAlreadyUsedException;
import com.taskhub.auth.domain.model.AuthenticatedUser;
import com.taskhub.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

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
        String canonicalEmail = canonical(email);
        if (users.existsByEmail(canonicalEmail)) {
            throw new EmailAlreadyUsedException(canonicalEmail);
        }
        User saved = users.save(User.register(name, canonicalEmail, passwordHasher.hash(rawPassword)));
        return tokenIssuer.issue(saved.getId(), saved.getEmail(), saved.getRole());
    }

    @Override
    public String login(String email, String rawPassword) {
        String canonicalEmail = canonical(email);
        AuthenticatedUser user = authenticator.authenticate(canonicalEmail, rawPassword);
        return tokenIssuer.issue(user.id(), canonicalEmail, user.role());
    }

    // One identity per person: "Eva@TaskHub.com " and "eva@taskhub.com" are the same account,
    // whatever the database collation says.
    private static String canonical(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
