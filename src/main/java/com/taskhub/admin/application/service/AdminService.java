package com.taskhub.admin.application.service;

import com.taskhub.admin.application.port.in.AdminUseCase;
import com.taskhub.auth.application.port.out.UserRepositoryPort;
import com.taskhub.auth.domain.model.User;
import com.taskhub.task.application.port.out.TaskRepositoryPort;
import com.taskhub.task.domain.model.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminService implements AdminUseCase {

    private final UserRepositoryPort userRepository;
    private final TaskRepositoryPort taskRepository;

    public AdminService(UserRepositoryPort userRepository, TaskRepositoryPort taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<Task> listTasks() {
        return taskRepository.findAll();
    }
}
