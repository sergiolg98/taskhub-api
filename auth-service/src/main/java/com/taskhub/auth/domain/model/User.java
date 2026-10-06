package com.taskhub.auth.domain.model;

import java.time.LocalDateTime;

public class User {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final LocalDateTime createdAt;

    public User(Long id, String name, String email, String password, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static User register(String name, String email, String encodedPassword) {
        return new User(null, name, email, encodedPassword, Role.USER, LocalDateTime.now());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
