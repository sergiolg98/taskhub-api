package com.taskhub.common.security;

/**
 * Identity of the caller, built only from the claims of a valid token (no database lookup).
 * A service that validates tokens does not need to own the users table.
 */
public record JwtPrincipal(Long userId, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
