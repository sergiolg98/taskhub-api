package com.taskhub.application.port.out;

import com.taskhub.domain.model.Task;

public interface NotificationPort {

    void notifyTaskCreated(Task task);
}
