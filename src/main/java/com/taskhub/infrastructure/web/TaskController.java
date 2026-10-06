package com.taskhub.infrastructure.web;

import com.taskhub.application.port.in.TaskUseCase;
import com.taskhub.infrastructure.security.SecurityUser;
import com.taskhub.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskUseCase taskUseCase;

    public TaskController(TaskUseCase taskUseCase) {
        this.taskUseCase = taskUseCase;
    }

    @GetMapping
    public List<TaskResponse> list(@AuthenticationPrincipal SecurityUser principal) {
        return taskUseCase.listMine(principal.toAuthenticatedUser()).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, @AuthenticationPrincipal SecurityUser principal) {
        return TaskResponse.from(taskUseCase.getById(id, principal.toAuthenticatedUser()));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request,
                                               @AuthenticationPrincipal SecurityUser principal) {
        TaskResponse created = TaskResponse.from(
                taskUseCase.create(request.title(), request.description(), principal.toAuthenticatedUser()));
        return ResponseEntity.created(URI.create("/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request,
                               @AuthenticationPrincipal SecurityUser principal) {
        return TaskResponse.from(taskUseCase.update(id, request.title(), request.description(),
                principal.toAuthenticatedUser()));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request,
                                     @AuthenticationPrincipal SecurityUser principal) {
        return TaskResponse.from(taskUseCase.changeStatus(id, request.status(), principal.toAuthenticatedUser()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal SecurityUser principal) {
        taskUseCase.delete(id, principal.toAuthenticatedUser());
    }
}
