package com.taskhub.auth.infrastructure.web.dto;

import com.taskhub.auth.domain.model.Role;
import com.taskhub.auth.domain.model.UserSummary;

// Contract of GET /users/{id}. Owned by auth; consumers keep their own copy.
public record UserSummaryResponse(Long id, String name, Role role) {

    public static UserSummaryResponse from(UserSummary s) {
        return new UserSummaryResponse(s.id(), s.name(), s.role());
    }
}
