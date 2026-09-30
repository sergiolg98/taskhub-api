package com.taskhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataTaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByOwnerId(Long ownerId);
}
