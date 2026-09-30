package com.taskhub.application.service;

import com.taskhub.application.port.in.TaskUseCase;
import com.taskhub.application.port.out.TaskRepositoryPort;
import com.taskhub.application.port.out.UserRepositoryPort;
import com.taskhub.domain.exception.TaskNotFoundException;
import com.taskhub.domain.exception.UserNotFoundException;
import com.taskhub.domain.model.Task;
import com.taskhub.domain.model.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService implements TaskUseCase {

    private final TaskRepositoryPort taskRepository;
    private final UserRepositoryPort userRepository;

    public TaskService(TaskRepositoryPort taskRepository, UserRepositoryPort userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Task create(String title, String description, Long ownerId) {
        userRepository.findById(ownerId).orElseThrow(() -> new UserNotFoundException(ownerId));
        return taskRepository.save(Task.create(title, description, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Task getById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> listByOwner(Long ownerId) {
        return taskRepository.findByOwnerId(ownerId);
    }

    @Override
    public Task update(Long id, String title, String description) {
        Task task = getById(id);
        task.update(title, description);
        return taskRepository.save(task);
    }

    @Override
    public Task changeStatus(Long id, TaskStatus status) {
        Task task = getById(id);
        task.changeStatus(status);
        return taskRepository.save(task);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        taskRepository.deleteById(id);
    }
}
