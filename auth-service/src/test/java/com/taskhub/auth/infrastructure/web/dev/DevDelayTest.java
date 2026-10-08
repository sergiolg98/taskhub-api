package com.taskhub.auth.infrastructure.web.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevDelayTest {

    @Autowired
    MockMvc mvc;

    @Test
    void adminCanSlowDownUserLookupsAndClearItAgain() throws Exception {
        String ana = "Bearer " + token("ana@taskhub.com", "abc123");
        String luis = "Bearer " + token("luis@taskhub.com", "def456");

        mvc.perform(post("/admin/dev/delay").param("ms", "300").header("Authorization", luis))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/dev/delay").param("ms", "300").header("Authorization", ana))
                .andExpect(status().isOk());

        long start = System.nanoTime();
        mvc.perform(get("/users/2").header("Authorization", luis)).andExpect(status().isOk());
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(300));

        mvc.perform(delete("/admin/dev/delay").header("Authorization", ana)).andExpect(status().isOk());
        start = System.nanoTime();
        mvc.perform(get("/users/2").header("Authorization", luis)).andExpect(status().isOk());
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(300));
    }

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(body).get("token").asText();
    }
}
