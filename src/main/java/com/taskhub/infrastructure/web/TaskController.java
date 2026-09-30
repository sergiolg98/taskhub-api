package com.taskhub.infrastructure.web;

import com.taskhub.application.port.in.TaskUseCase;
import com.taskhub.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Versión base: sin autenticación, el dueño se filtra con ?ownerId=. Con JWT saldrá del token.
    @GetMapping
    public List<TaskResponse> list(@RequestParam Long ownerId) {
        return taskUseCase.listByOwner(ownerId).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return TaskResponse.from(taskUseCase.getById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = TaskResponse.from(
                taskUseCase.create(request.title(), request.description(), request.ownerId()));
        return ResponseEntity.created(URI.create("/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(taskUseCase.update(id, request.title(), request.description()));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return TaskResponse.from(taskUseCase.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskUseCase.delete(id);
    }
}
