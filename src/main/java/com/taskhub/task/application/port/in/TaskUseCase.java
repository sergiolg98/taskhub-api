package com.taskhub.task.application.port.in;

import com.taskhub.task.domain.model.Requester;
import com.taskhub.task.domain.model.Task;
import com.taskhub.task.domain.model.TaskStatus;

import java.util.List;

public interface TaskUseCase {

    Task create(String title, String description, Requester requester);

    Task getById(Long id, Requester requester);

    List<Task> listMine(Requester requester);

    Task update(Long id, String title, String description, Requester requester);

    Task changeStatus(Long id, TaskStatus status, Requester requester);

    void delete(Long id, Requester requester);
}
