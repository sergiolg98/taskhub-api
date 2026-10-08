package com.taskhub.task.infrastructure.web;

import com.taskhub.common.security.JwtPrincipal;
import com.taskhub.task.application.port.in.TaskUseCase;
import com.taskhub.task.domain.model.Requester;
import com.taskhub.task.infrastructure.web.dto.*;
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
    public List<TaskResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return taskUseCase.listMine(requester(principal)).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal principal) {
        return TaskResponse.from(taskUseCase.getById(id, requester(principal)));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request,
                                               @AuthenticationPrincipal JwtPrincipal principal) {
        TaskResponse created = TaskResponse.from(
                taskUseCase.create(request.title(), request.description(), requester(principal)));
        return ResponseEntity.created(URI.create("/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request,
                               @AuthenticationPrincipal JwtPrincipal principal) {
        return TaskResponse.from(taskUseCase.update(id, request.title(), request.description(),
                requester(principal)));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request,
                                     @AuthenticationPrincipal JwtPrincipal principal) {
        return TaskResponse.from(taskUseCase.changeStatus(id, request.status(), requester(principal)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal principal) {
        taskUseCase.delete(id, requester(principal));
    }

    private static Requester requester(JwtPrincipal principal) {
        return new Requester(principal.userId(), principal.isAdmin());
    }
}
