package com.taskhub;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real Feign path: task-service -> HTTP -> a fake auth-service (a JDK HTTP server) that answers what each test wants.
@SpringBootTest
@AutoConfigureMockMvc
class OwnerVerificationTest {

    private interface Behaviour {
        void answer(HttpExchange exchange) throws Exception;
    }

    private static final HttpServer fakeAuth;
    private static volatile Behaviour behaviour;
    private static final List<String> received = new CopyOnWriteArrayList<>();

    static {
        try {
            fakeAuth = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            fakeAuth.createContext("/", exchange -> {
                try {
                    received.add(exchange.getRequestURI().getPath()
                            + " authorization=" + exchange.getRequestHeaders().getFirst("Authorization")
                            + " x-request-id=" + exchange.getRequestHeaders().getFirst("X-Request-Id"));
                    behaviour.answer(exchange);
                } catch (Exception e) {
                    exchange.close();
                }
            });
            fakeAuth.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void authUrl(DynamicPropertyRegistry registry) {
        registry.add("taskhub.auth-service.url", () -> "http://localhost:" + fakeAuth.getAddress().getPort());
        registry.add("spring.cloud.openfeign.client.config.default.read-timeout", () -> "500");
    }

    @AfterAll
    static void stop() {
        fakeAuth.stop(0);
    }

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() {
        received.clear();
        behaviour = ex -> reply(ex, 200, "{\"id\":2,\"name\":\"Luis User\",\"role\":\"USER\"}");
    }

    @Test
    void existingOwnerCreatesTheTaskAndTheCallerHeadersTravelToAuth() throws Exception {
        String token = TestTokens.luis();

        createTask(token).andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(2));

        assertThat(received).containsExactly("/users/2 authorization=" + token + " x-request-id=trace-1");
    }

    @Test
    void unknownOwnerIs404WithTheDomainMessage() throws Exception {
        behaviour = ex -> reply(ex, 404, "{\"status\":404,\"message\":\"User not found: 2\"}");

        createTask(TestTokens.luis()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Owner not found: 2"));
    }

    @Test
    void authAnsweringAnErrorIs503NotA500AndNothingIsSaved() throws Exception {
        behaviour = ex -> reply(ex, 500, "<html>boom, not json</html>");

        createTask(TestTokens.luis()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("User verification is temporarily unavailable"));
    }

    @Test
    void authTooSlowIs503() throws Exception {
        behaviour = ex -> {
            Thread.sleep(1500);
            reply(ex, 200, "{\"id\":2,\"name\":\"Luis User\",\"role\":\"USER\"}");
        };

        createTask(TestTokens.luis()).andExpect(status().isServiceUnavailable());
    }

    @Test
    void authDroppingTheConnectionIs503() throws Exception {
        behaviour = HttpExchange::close;

        createTask(TestTokens.luis()).andExpect(status().isServiceUnavailable());
    }

    private ResultActions createTask(String token) throws Exception {
        return mvc.perform(post("/tasks").header("Authorization", token).header("X-Request-Id", "trace-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\"}"));
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
