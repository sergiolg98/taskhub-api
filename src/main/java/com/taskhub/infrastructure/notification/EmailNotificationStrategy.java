package com.taskhub.infrastructure.notification;

import com.taskhub.domain.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Simulated email: there is no SMTP server, the "sending" is only logged.
@Component
class EmailNotificationStrategy implements NotificationStrategy {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationStrategy.class);

    @Override
    public String type() {
        return "email";
    }

    @Override
    public void notifyTaskCreated(Task task) {
        log.info("[simulated email] To owner {}: your task '{}' was created", task.getOwnerId(), task.getTitle());
    }
}
