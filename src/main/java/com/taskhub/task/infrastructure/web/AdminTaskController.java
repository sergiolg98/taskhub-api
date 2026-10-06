package com.taskhub.task.infrastructure.web;

import com.taskhub.task.application.port.in.ListAllTasksUseCase;
import com.taskhub.task.infrastructure.web.dto.TaskResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTaskController {

    private final ListAllTasksUseCase listAllTasks;

    public AdminTaskController(ListAllTasksUseCase listAllTasks) {
        this.listAllTasks = listAllTasks;
    }

    @GetMapping
    public List<TaskResponse> tasks() {
        return listAllTasks.listAllTasks().stream().map(TaskResponse::from).toList();
    }
}
