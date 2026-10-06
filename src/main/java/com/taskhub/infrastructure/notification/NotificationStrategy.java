package com.taskhub.infrastructure.notification;

import com.taskhub.domain.model.Task;

public interface NotificationStrategy {

    /** Value of {@code taskhub.notifications.type} that selects this strategy. */
    String type();

    void notifyTaskCreated(Task task);
}
