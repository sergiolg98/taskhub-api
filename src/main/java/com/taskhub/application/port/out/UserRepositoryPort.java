package com.taskhub.application.port.out;

import com.taskhub.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findById(Long id);
}
