package com.taskhub.auth.infrastructure.web.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Admin-only (the /admin/** rule) and dev-only (@Profile): it must never exist in test or prod.
@Profile("dev")
@RestController
@RequestMapping("/admin/dev/delay")
class DevDelayController {

    private final DevDelay delay;

    DevDelayController(DevDelay delay) {
        this.delay = delay;
    }

    @PostMapping
    Map<String, Long> set(@RequestParam long ms) {
        delay.set(Math.max(0, Math.min(ms, 30_000)));
        return Map.of("delayMs", delay.get());
    }

    @DeleteMapping
    Map<String, Long> clear() {
        delay.set(0);
        return Map.of("delayMs", 0L);
    }
}
