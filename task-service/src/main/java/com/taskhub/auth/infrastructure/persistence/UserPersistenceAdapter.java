package com.taskhub.auth.infrastructure.persistence;

import com.taskhub.auth.application.port.out.UserRepositoryPort;
import com.taskhub.auth.domain.exception.EmailAlreadyUsedException;
import com.taskhub.auth.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;

    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return this.repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return this.repository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        try {
            return toDomain(this.repository.save(toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            // The UNIQUE constraint is the real guarantee: two simultaneous registrations can both pass existsByEmail.
            throw new EmailAlreadyUsedException(user.getEmail());
        }
    }

    @Override
    public List<User> findAll() {
        return this.repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    // private mapper to be used only in this class
    private User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRole(),
                entity.getCreatedAt());
    }

    private UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getCreatedAt());
    }
}
