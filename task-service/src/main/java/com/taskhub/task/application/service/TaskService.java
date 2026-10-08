package com.taskhub.task.application.service;

import com.taskhub.task.application.port.in.TaskUseCase;
import com.taskhub.task.application.port.out.NotificationPort;
import com.taskhub.task.application.port.out.TaskRepositoryPort;
import com.taskhub.task.application.port.out.UserLookupPort;
import com.taskhub.task.domain.exception.OwnerNotFoundException;
import com.taskhub.task.domain.exception.TaskNotFoundException;
import com.taskhub.task.domain.model.Requester;
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
    private final UserLookupPort userLookup;

    public TaskService(TaskRepositoryPort taskRepository, NotificationPort notifications, UserLookupPort userLookup) {
        this.taskRepository = taskRepository;
        this.notifications = notifications;
        this.userLookup = userLookup;
    }

    @Override
    public Task create(String title, String description, Requester requester) {
        // The token proves who the user WAS when it was issued; this asks auth-service whether they still exist.
        userLookup.findById(requester.userId()).orElseThrow(() -> new OwnerNotFoundException(requester.userId()));
        Task created = taskRepository.save(Task.create(title, description, requester.userId()));
        notifications.notifyTaskCreated(created);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Task getById(Long id, Requester requester) {
        return findAccessible(id, requester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> listMine(Requester requester) {
        return taskRepository.findByOwnerId(requester.userId());
    }

    @Override
    public Task update(Long id, String title, String description, Requester requester) {
        Task task = findAccessible(id, requester);
        task.update(title, description);
        return taskRepository.save(task);
    }

    @Override
    public Task changeStatus(Long id, TaskStatus status, Requester requester) {
        Task task = findAccessible(id, requester);
        task.changeStatus(status);
        return taskRepository.save(task);
    }

    @Override
    public void delete(Long id, Requester requester) {
        findAccessible(id, requester);
        taskRepository.deleteById(id);
    }

    // Ownership rule: only the owner or an ADMIN. Otherwise we answer as if the task did not exist (404).
    private Task findAccessible(Long id, Requester requester) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        if (!requester.admin() && !task.getOwnerId().equals(requester.userId())) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }
}
