package com.taskhub.infrastructure.notification;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Spring injects every NotificationStrategy bean, so adding a new strategy does not require touching this class (OCP).
@Component
public class NotificationStrategyFactory {

    private final Map<String, NotificationStrategy> strategies;

    public NotificationStrategyFactory(List<NotificationStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(NotificationStrategy::type, Function.identity()));
    }

    public NotificationStrategy forType(String type) {
        NotificationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unknown notification type '" + type + "'. Available: " + strategies.keySet());
        }
        return strategy;
    }
}
