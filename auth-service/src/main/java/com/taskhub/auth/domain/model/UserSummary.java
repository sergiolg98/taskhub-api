package com.taskhub.auth.domain.model;

/** Public view of a user: what other parts of the system may know. Never email or password. */
public record UserSummary(Long id, String name, Role role) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getRole());
    }
}
