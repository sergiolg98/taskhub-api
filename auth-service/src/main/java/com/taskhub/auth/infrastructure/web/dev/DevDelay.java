package com.taskhub.auth.infrastructure.web.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** The artificial delay (ms) applied to GET /users/**. Exists only in the dev profile, to demo a slow dependency in class 11. */
@Profile("dev")
@Component
class DevDelay {

    private final AtomicLong millis = new AtomicLong();

    long get() {
        return millis.get();
    }

    void set(long value) {
        millis.set(value);
    }
}
