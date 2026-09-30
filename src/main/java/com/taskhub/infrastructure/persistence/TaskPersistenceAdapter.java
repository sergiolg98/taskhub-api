package com.taskhub.infrastructure.persistence;

import com.taskhub.application.port.out.TaskRepositoryPort;
import com.taskhub.domain.model.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TaskPersistenceAdapter implements TaskRepositoryPort {

    private final SpringDataTaskRepository repository;

    public TaskPersistenceAdapter(SpringDataTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public Task save(Task task) {
        return toDomain(repository.save(toEntity(task)));
    }

    @Override
    public Optional<Task> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Task> findByOwnerId(Long ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private TaskEntity toEntity(Task t) {
        return new TaskEntity(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getOwnerId(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private Task toDomain(TaskEntity e) {
        return new Task(e.getId(), e.getTitle(), e.getDescription(), e.getStatus(),
                e.getOwnerId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
