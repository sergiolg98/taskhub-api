package com.taskhub;

import com.sun.net.httpserver.HttpExchange;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Timeout, Retry, Circuit Breaker and Fallback around the call to auth-service, driven end to end through the API.
// Test settings (src/test/resources): 2 attempts, window of 4 calls, 1 s in OPEN, 2 calls allowed in HALF_OPEN.
@SpringBootTest
@AutoConfigureMockMvc
class ResilienceTest {

    private static final FakeAuthServer auth = new FakeAuthServer();

    @DynamicPropertySource
    static void authUrl(DynamicPropertyRegistry registry) {
        registry.add("taskhub.auth-service.url", auth::url);
        registry.add("spring.cloud.openfeign.client.config.default.read-timeout", () -> "300");
    }

    @AfterAll
    static void stop() {
        auth.stop();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    CircuitBreakerRegistry registry;

    CircuitBreaker breaker;

    @BeforeEach
    void reset() {
        auth.reset();
        breaker = registry.circuitBreaker("authService");
        breaker.reset();
    }

    @Test
    void aTransientFailureIsRetriedAndTheOwnerEndsVerified() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        auth.behaviour = ex -> {
            if (calls.incrementAndGet() == 1) {
                FakeAuthServer.reply(ex, 500, "{}");
            } else {
                FakeAuthServer.reply(ex, 200, FakeAuthServer.LUIS_JSON);
            }
        };

        createTask().andExpect(status().isCreated()).andExpect(jsonPath("$.ownerVerified").value(true));

        assertThat(auth.hits.get()).isEqualTo(2);
    }

    @Test
    void whenAuthKeepsFailingTheTaskIsAcceptedUnverifiedAfterTheRetries() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 503, "{}");

        createTask().andExpect(status().isCreated()).andExpect(jsonPath("$.ownerVerified").value(false));

        assertThat(auth.hits.get()).isEqualTo(2);
    }

    @Test
    void aSlowAuthIsCutByTheTimeoutInsteadOfHangingTheRequest() throws Exception {
        auth.behaviour = ex -> {
            Thread.sleep(1500);
            FakeAuthServer.reply(ex, 200, FakeAuthServer.LUIS_JSON);
        };

        long start = System.nanoTime();
        createTask().andExpect(status().isCreated()).andExpect(jsonPath("$.ownerVerified").value(false));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1400));
    }

    @Test
    void theCircuitOpensAfterRepeatedFailuresAndThenAnswersWithoutCallingAuth() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 500, "{}");

        createTask();   // 2 attempts = 2 failures
        createTask();   // 2 more: the window of 4 is full and 100 % failed
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int hitsWhenOpened = auth.hits.get();
        createTask().andExpect(status().isCreated()).andExpect(jsonPath("$.ownerVerified").value(false));
        createTask().andExpect(status().isCreated());

        assertThat(auth.hits.get()).as("no call reaches auth while OPEN, and no retry is attempted").isEqualTo(hitsWhenOpened);
    }

    @Test
    void afterTheWaitTheCircuitProbesAndClosesWhenAuthIsBack() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 500, "{}");
        createTask();
        createTask();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        auth.behaviour = ex -> FakeAuthServer.reply(ex, 200, FakeAuthServer.LUIS_JSON);
        await().atMost(Duration.ofSeconds(5)).until(() -> breaker.getState() == CircuitBreaker.State.HALF_OPEN);

        createTask().andExpect(jsonPath("$.ownerVerified").value(true));
        createTask().andExpect(jsonPath("$.ownerVerified").value(true));
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void aMissingUserIsAnAnswerNotAFailure() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 404, "{}");

        for (int i = 0; i < 6; i++) {
            createTask().andExpect(status().isNotFound());
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(auth.hits.get()).as("a 404 is never retried").isEqualTo(6);
    }

    @Test
    void aRejectionFromAuthIsNeitherRetriedNorCounted() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 401, "{}");

        for (int i = 0; i < 6; i++) {
            createTask().andExpect(status().isCreated()).andExpect(jsonPath("$.ownerVerified").value(false));
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(auth.hits.get()).isEqualTo(6);
    }

    @Test
    void readsDoNotDependOnAuthAtAll() throws Exception {
        auth.behaviour = HttpExchange::close;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tasks")
                        .header("Authorization", TestTokens.luis()))
                .andExpect(status().isOk());

        assertThat(auth.hits.get()).isZero();
    }

    private ResultActions createTask() throws Exception {
        return mvc.perform(post("/tasks").header("Authorization", TestTokens.luis())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\"}"));
    }
}
