package com.taskhub.task.application.port.in;

import com.taskhub.auth.domain.model.AuthenticatedUser;
import com.taskhub.task.domain.model.Task;
import com.taskhub.task.domain.model.TaskStatus;

import java.util.List;

public interface TaskUseCase {

    Task create(String title, String description, AuthenticatedUser user);

    Task getById(Long id, AuthenticatedUser user);

    List<Task> listMine(AuthenticatedUser user);

    Task update(Long id, String title, String description, AuthenticatedUser user);

    Task changeStatus(Long id, TaskStatus status, AuthenticatedUser user);

    void delete(Long id, AuthenticatedUser user);
}
