package com.taskhub.auth.application.port.in;

import com.taskhub.auth.domain.model.User;

import java.util.List;

public interface ListUsersUseCase {

    List<User> listUsers();
}
