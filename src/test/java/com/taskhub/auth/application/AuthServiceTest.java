package com.taskhub.auth.application;

import com.taskhub.auth.application.port.out.CredentialsAuthenticatorPort;
import com.taskhub.auth.application.port.out.PasswordHasherPort;
import com.taskhub.auth.application.port.out.TokenIssuerPort;
import com.taskhub.auth.application.port.out.UserRepositoryPort;
import com.taskhub.auth.application.service.AuthService;
import com.taskhub.auth.domain.exception.EmailAlreadyUsedException;
import com.taskhub.auth.domain.exception.InvalidCredentialsException;
import com.taskhub.auth.domain.model.AuthenticatedUser;
import com.taskhub.auth.domain.model.Role;
import com.taskhub.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pure unit test: no Spring context and no HTTP. This is possible because the use case only knows ports.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepositoryPort users;
    @Mock PasswordHasherPort passwordHasher;
    @Mock CredentialsAuthenticatorPort authenticator;
    @Mock TokenIssuerPort tokenIssuer;
    @InjectMocks AuthService authService;

    @Test
    void registerAlwaysCreatesAUserWithEncodedPassword() {
        when(users.existsByEmail("eva@taskhub.com")).thenReturn(false);
        when(passwordHasher.hash("Secret123")).thenReturn("hashed");
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(10L, u.getName(), u.getEmail(), u.getPassword(), u.getRole(), LocalDateTime.now());
        });
        when(tokenIssuer.issue(10L, "eva@taskhub.com", Role.USER)).thenReturn("token");

        String token = authService.register("Eva", "eva@taskhub.com", "Secret123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(token).isEqualTo("token");
    }

    @Test
    void registerRejectsDuplicatedEmail() {
        when(users.existsByEmail("eva@taskhub.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Eva", "eva@taskhub.com", "Secret123"))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(users, never()).save(any());
    }

    @Test
    void loginIssuesATokenWithTheRoleOfTheAuthenticatedUser() {
        when(authenticator.authenticate("ana@taskhub.com", "abc123"))
                .thenReturn(new AuthenticatedUser(1L, Role.ADMIN));
        when(tokenIssuer.issue(1L, "ana@taskhub.com", Role.ADMIN)).thenReturn("admin-token");

        assertThat(authService.login("ana@taskhub.com", "abc123")).isEqualTo("admin-token");
    }

    @Test
    void loginDoesNotIssueATokenWhenCredentialsAreWrong() {
        when(authenticator.authenticate("ana@taskhub.com", "bad")).thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> authService.login("ana@taskhub.com", "bad"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenIssuer, never()).issue(any(), any(), any());
    }
}
