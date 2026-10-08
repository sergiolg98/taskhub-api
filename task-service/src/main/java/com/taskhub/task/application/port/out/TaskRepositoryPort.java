package com.taskhub.task.application.port.out;

import com.taskhub.task.domain.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepositoryPort {

    Task save(Task task);

    Optional<Task> findById(Long id);

    List<Task> findByOwnerId(Long ownerId);

    List<Task> findAll();

    void deleteById(Long id);
}
