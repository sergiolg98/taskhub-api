package com.taskhub.infrastructure.web;

import com.taskhub.application.port.in.AdminUseCase;
import com.taskhub.infrastructure.web.dto.TaskResponse;
import com.taskhub.infrastructure.web.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUseCase adminUseCase;

    public AdminController(AdminUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return adminUseCase.listUsers().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/tasks")
    public List<TaskResponse> tasks() {
        return adminUseCase.listTasks().stream().map(TaskResponse::from).toList();
    }
}
