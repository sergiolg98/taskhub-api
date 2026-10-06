package com.taskhub.task.domain;

import com.taskhub.task.domain.exception.InvalidTaskException;
import com.taskhub.task.domain.model.Task;
import com.taskhub.task.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    @Test
    void newTasksStartPending() {
        assertThat(Task.create("Preparar clase", null, 2L).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void aBlankTitleIsRejectedOnCreateAndOnUpdate() {
        assertThatThrownBy(() -> Task.create(" ", null, 2L)).isInstanceOf(InvalidTaskException.class);
        Task task = Task.create("ok", null, 2L);
        assertThatThrownBy(() -> task.update("", null)).isInstanceOf(InvalidTaskException.class);
    }
}
