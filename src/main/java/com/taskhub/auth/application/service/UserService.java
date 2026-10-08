package com.taskhub.auth.application.service;

import com.taskhub.auth.application.port.in.GetUserSummaryUseCase;
import com.taskhub.auth.application.port.in.ListUsersUseCase;
import com.taskhub.auth.application.port.out.UserRepositoryPort;
import com.taskhub.auth.domain.exception.UserNotFoundException;
import com.taskhub.auth.domain.model.User;
import com.taskhub.auth.domain.model.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService implements ListUsersUseCase, GetUserSummaryUseCase {

    private final UserRepositoryPort users;

    public UserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public List<User> listUsers() {
        return users.findAll();
    }

    @Override
    public UserSummary getSummary(Long id) {
        return users.findById(id).map(UserSummary::from).orElseThrow(() -> new UserNotFoundException(id));
    }
}
