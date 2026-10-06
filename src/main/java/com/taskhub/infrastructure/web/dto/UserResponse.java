package com.taskhub.infrastructure.web.dto;

import com.taskhub.domain.model.Role;
import com.taskhub.domain.model.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, Role role, LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
