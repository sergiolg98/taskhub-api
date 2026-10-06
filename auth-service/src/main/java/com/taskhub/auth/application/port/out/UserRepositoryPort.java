package com.taskhub.auth.application.port.out;

import com.taskhub.auth.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    List<User> findAll();
}
