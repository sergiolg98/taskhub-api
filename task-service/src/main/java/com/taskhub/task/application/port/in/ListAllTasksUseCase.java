package com.taskhub.task.application.port.in;

import com.taskhub.task.domain.model.Task;

import java.util.List;

public interface ListAllTasksUseCase {

    List<Task> listAllTasks();
}
