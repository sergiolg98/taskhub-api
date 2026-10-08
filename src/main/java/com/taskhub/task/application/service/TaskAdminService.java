package com.taskhub.task.application.service;

import com.taskhub.task.application.port.in.ListAllTasksUseCase;
import com.taskhub.task.application.port.out.TaskRepositoryPort;
import com.taskhub.task.domain.model.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskAdminService implements ListAllTasksUseCase {

    private final TaskRepositoryPort tasks;

    public TaskAdminService(TaskRepositoryPort tasks) {
        this.tasks = tasks;
    }

    @Override
    public List<Task> listAllTasks() {
        return tasks.findAll();
    }
}
