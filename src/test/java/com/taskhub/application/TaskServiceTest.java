package com.taskhub.application;

import com.taskhub.application.port.out.NotificationPort;
import com.taskhub.application.port.out.TaskRepositoryPort;
import com.taskhub.application.service.TaskService;
import com.taskhub.domain.exception.TaskNotFoundException;
import com.taskhub.domain.model.AuthenticatedUser;
import com.taskhub.domain.model.Role;
import com.taskhub.domain.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final AuthenticatedUser LUIS = new AuthenticatedUser(2L, Role.USER);
    private static final AuthenticatedUser EVA = new AuthenticatedUser(3L, Role.USER);
    private static final AuthenticatedUser ANA = new AuthenticatedUser(1L, Role.ADMIN);

    @Mock TaskRepositoryPort tasks;
    @Mock NotificationPort notifications;
    @InjectMocks TaskService service;

    @Test
    void createStoresTheTaskForTheAuthenticatedUserAndNotifies() {
        when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task created = service.create("Preparar clase", null, LUIS);

        assertThat(created.getOwnerId()).isEqualTo(2L);
        verify(notifications).notifyTaskCreated(created);
    }

    @Test
    void anotherUserSeesTheTaskAsNonExistent() {
        when(tasks.findById(7L)).thenReturn(Optional.of(Task.create("Privada", null, 2L)));

        assertThatThrownBy(() -> service.getById(7L, EVA)).isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> service.delete(7L, EVA)).isInstanceOf(TaskNotFoundException.class);
        verify(tasks, never()).deleteById(any());
    }

    @Test
    void adminCanAccessAnyTask() {
        when(tasks.findById(7L)).thenReturn(Optional.of(Task.create("Privada", null, 2L)));

        assertThat(service.getById(7L, ANA).getTitle()).isEqualTo("Privada");
    }
}
