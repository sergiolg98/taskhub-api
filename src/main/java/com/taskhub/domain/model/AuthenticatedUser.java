package com.taskhub.domain.model;

public record AuthenticatedUser(Long id, Role role) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
