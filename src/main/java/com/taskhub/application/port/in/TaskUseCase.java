package com.taskhub.application.port.in;

import com.taskhub.domain.model.Task;
import com.taskhub.domain.model.TaskStatus;

import java.util.List;

public interface TaskUseCase {

    Task create(String title, String description, Long ownerId);

    Task getById(Long id);

    List<Task> listByOwner(Long ownerId);

    Task update(Long id, String title, String description);

    Task changeStatus(Long id, TaskStatus status);

    void delete(Long id);
}
