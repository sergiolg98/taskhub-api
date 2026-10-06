package com.taskhub.task.application;

import com.taskhub.task.application.port.out.NotificationPort;
import com.taskhub.task.application.port.out.TaskRepositoryPort;
import com.taskhub.task.application.port.out.UserLookupPort;
import com.taskhub.task.domain.exception.ExternalServiceException;
import com.taskhub.task.domain.exception.OwnerNotFoundException;
import com.taskhub.task.application.service.TaskService;
import com.taskhub.task.domain.exception.TaskNotFoundException;
import com.taskhub.task.domain.model.Requester;
import com.taskhub.task.domain.model.Task;
import com.taskhub.task.domain.model.UserSummary;
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

    private static final Requester LUIS = new Requester(2L, false);
    private static final Requester EVA = new Requester(3L, false);
    private static final Requester ANA = new Requester(1L, true);

    @Mock TaskRepositoryPort tasks;
    @Mock NotificationPort notifications;
    @Mock UserLookupPort userLookup;
    @InjectMocks TaskService service;

    @Test
    void createStoresTheTaskForTheRequesterAndNotifies() {
        when(userLookup.findById(2L)).thenReturn(Optional.of(new UserSummary(2L, "Luis User", "USER")));
        when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task created = service.create("Preparar clase", null, LUIS);

        assertThat(created.getOwnerId()).isEqualTo(2L);
        verify(notifications).notifyTaskCreated(created);
    }

    @Test
    void createFailsWhenTheOwnerDoesNotExist() {
        when(userLookup.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Preparar clase", null, LUIS))
                .isInstanceOf(OwnerNotFoundException.class);
        verify(tasks, never()).save(any());
        verify(notifications, never()).notifyTaskCreated(any());
    }

    @Test
    void createFailsWithoutSavingWhenTheLookupCannotAnswer() {
        when(userLookup.findById(2L)).thenThrow(new ExternalServiceException("auth-service is not reachable"));

        assertThatThrownBy(() -> service.create("Preparar clase", null, LUIS))
                .isInstanceOf(ExternalServiceException.class);
        verify(tasks, never()).save(any());
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
