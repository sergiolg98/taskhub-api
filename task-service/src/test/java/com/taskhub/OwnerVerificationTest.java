package com.taskhub;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real Feign path against a fake auth-service: what travels, and how each answer maps to the API response.
@SpringBootTest
@AutoConfigureMockMvc
class OwnerVerificationTest {

    private static final FakeAuthServer auth = new FakeAuthServer();

    @DynamicPropertySource
    static void authUrl(DynamicPropertyRegistry registry) {
        registry.add("taskhub.auth-service.url", auth::url);
    }

    @AfterAll
    static void stop() {
        auth.stop();
    }

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() {
        auth.reset();
    }

    @Test
    void existingOwnerCreatesAVerifiedTaskAndTheCallerHeadersTravelToAuth() throws Exception {
        String token = TestTokens.luis();

        createTask(token).andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(2))
                .andExpect(jsonPath("$.ownerVerified").value(true));

        assertThat(auth.received).containsExactly("/users/2 authorization=" + token + " x-request-id=trace-1");
    }

    @Test
    void unknownOwnerIs404WithTheDomainMessage() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 404, "{\"status\":404,\"message\":\"User not found: 2\"}");

        createTask(TestTokens.luis()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Owner not found: 2"));
    }

    @Test
    void authAnsweringAnErrorDoesNotBlockTheTaskItIsAcceptedUnverified() throws Exception {
        auth.behaviour = ex -> FakeAuthServer.reply(ex, 500, "<html>boom, not json</html>");

        createTask(TestTokens.luis()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerVerified").value(false));
    }

    private ResultActions createTask(String token) throws Exception {
        return mvc.perform(post("/tasks").header("Authorization", token).header("X-Request-Id", "trace-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\"}"));
    }
}
