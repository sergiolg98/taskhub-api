package com.taskhub.auth.application.service;

import com.taskhub.auth.application.port.in.ListUsersUseCase;
import com.taskhub.auth.application.port.out.UserRepositoryPort;
import com.taskhub.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService implements ListUsersUseCase {

    private final UserRepositoryPort users;

    public UserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public List<User> listUsers() {
        return users.findAll();
    }
}
