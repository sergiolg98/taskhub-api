package com.taskhub.infrastructure.notification;

import com.taskhub.application.port.out.NotificationPort;
import com.taskhub.domain.model.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Connects the application port with the strategy chosen by configuration.
// An unknown type fails at startup instead of at the first notification.
@Component
class NotificationAdapter implements NotificationPort {

    private final NotificationStrategy strategy;

    NotificationAdapter(NotificationStrategyFactory factory,
                        @Value("${taskhub.notifications.type:log}") String type) {
        this.strategy = factory.forType(type);
    }

    @Override
    public void notifyTaskCreated(Task task) {
        strategy.notifyTaskCreated(task);
    }
}
