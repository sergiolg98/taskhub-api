package com.taskhub.task.application.port.out;

import com.taskhub.task.domain.model.Task;

public interface NotificationPort {

    void notifyTaskCreated(Task task);
}
