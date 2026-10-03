package com.taskhub.application.service;

import com.taskhub.application.port.in.AdminUseCase;
import com.taskhub.application.port.out.TaskRepositoryPort;
import com.taskhub.application.port.out.UserRepositoryPort;
import com.taskhub.domain.model.Task;
import com.taskhub.domain.model.User;
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
