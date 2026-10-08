package com.taskhub.auth.infrastructure.web;

import com.taskhub.auth.application.port.in.ListUsersUseCase;
import com.taskhub.auth.infrastructure.web.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final ListUsersUseCase listUsers;

    public AdminUserController(ListUsersUseCase listUsers) {
        this.listUsers = listUsers;
    }

    @GetMapping
    public List<UserResponse> users() {
        return listUsers.listUsers().stream().map(UserResponse::from).toList();
    }
}
