package com.taskhub.task.infrastructure.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationStrategyFactoryTest {

    private final NotificationStrategyFactory factory = new NotificationStrategyFactory(
            List.of(new LogNotificationStrategy(), new EmailNotificationStrategy()));

    @Test
    void returnsTheStrategyForEachType() {
        assertThat(factory.forType("log")).isInstanceOf(LogNotificationStrategy.class);
        assertThat(factory.forType("email")).isInstanceOf(EmailNotificationStrategy.class);
    }

    @Test
    void unknownTypeIsRejectedAndListsTheAvailableOnes() {
        assertThatThrownBy(() -> factory.forType("sms"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sms")
                .hasMessageContaining("log");
    }
}
