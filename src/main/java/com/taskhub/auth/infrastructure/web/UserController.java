package com.taskhub.auth.infrastructure.web;

import com.taskhub.auth.application.port.in.GetUserSummaryUseCase;
import com.taskhub.auth.infrastructure.web.dto.UserSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final GetUserSummaryUseCase getUserSummary;

    public UserController(GetUserSummaryUseCase getUserSummary) {
        this.getUserSummary = getUserSummary;
    }

    // Anyone may look up themselves; only an ADMIN may look up other users.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public UserSummaryResponse get(@PathVariable Long id) {
        return UserSummaryResponse.from(getUserSummary.getSummary(id));
    }
}
