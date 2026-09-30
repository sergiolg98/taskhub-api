package com.taskhub.infrastructure.persistence;

import com.taskhub.application.port.out.UserRepositoryPort;
import com.taskhub.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;

    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(e -> new User(e.getId(), e.getName(), e.getEmail(),
                e.getPassword(), e.getRole(), e.getCreatedAt()));
    }
}
