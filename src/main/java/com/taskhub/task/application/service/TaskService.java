package com.taskhub.task.application.service;

import com.taskhub.auth.domain.model.AuthenticatedUser;
import com.taskhub.task.application.port.in.TaskUseCase;
import com.taskhub.task.application.port.out.NotificationPort;
import com.taskhub.task.application.port.out.TaskRepositoryPort;
import com.taskhub.task.domain.exception.TaskNotFoundException;
import com.taskhub.task.domain.model.Task;
import com.taskhub.task.domain.model.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService implements TaskUseCase {

    private final TaskRepositoryPort taskRepository;
    private final NotificationPort notifications;

    public TaskService(TaskRepositoryPort taskRepository, NotificationPort notifications) {
        this.taskRepository = taskRepository;
        this.notifications = notifications;
    }

    @Override
    public Task create(String title, String description, AuthenticatedUser user) {
        Task created = taskRepository.save(Task.create(title, description, user.id()));
        notifications.notifyTaskCreated(created);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Task getById(Long id, AuthenticatedUser user) {
        return findAccessible(id, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> listMine(AuthenticatedUser user) {
        return taskRepository.findByOwnerId(user.id());
    }

    @Override
    public Task update(Long id, String title, String description, AuthenticatedUser user) {
        Task task = findAccessible(id, user);
        task.update(title, description);
        return taskRepository.save(task);
    }

    @Override
    public Task changeStatus(Long id, TaskStatus status, AuthenticatedUser user) {
        Task task = findAccessible(id, user);
        task.changeStatus(status);
        return taskRepository.save(task);
    }

    @Override
    public void delete(Long id, AuthenticatedUser user) {
        findAccessible(id, user);
        taskRepository.deleteById(id);
    }

    // Ownership rule: only the owner or an ADMIN. Otherwise we answer as if the task did not exist (404).
    private Task findAccessible(Long id, AuthenticatedUser user) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        if (!user.isAdmin() && !task.getOwnerId().equals(user.id())) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }
}
