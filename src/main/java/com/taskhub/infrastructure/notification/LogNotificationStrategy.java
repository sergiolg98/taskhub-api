package com.taskhub.infrastructure.notification;

import com.taskhub.domain.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LogNotificationStrategy implements NotificationStrategy {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationStrategy.class);

    @Override
    public String type() {
        return "log";
    }

    @Override
    public void notifyTaskCreated(Task task) {
        log.info("Task created: id={} title='{}' ownerId={}", task.getId(), task.getTitle(), task.getOwnerId());
    }
}
