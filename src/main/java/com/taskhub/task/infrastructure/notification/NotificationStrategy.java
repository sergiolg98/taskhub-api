package com.taskhub.task.infrastructure.notification;

import com.taskhub.task.domain.model.Task;

public interface NotificationStrategy {

    /** Value of {@code taskhub.notifications.type} that selects this strategy. */
    String type();

    void notifyTaskCreated(Task task);
}
