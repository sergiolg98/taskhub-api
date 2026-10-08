package com.taskhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void servesTheSharedFileAndTheProfileFileOfAService() throws Exception {
        mvc.perform(get("/task-service/dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("task-service"))
                .andExpect(jsonPath("$.profiles[0]").value("dev"))
                // the profile file comes first: it wins over the shared one
                .andExpect(jsonPath("$.propertySources[0].source['spring.datasource.url']")
                        .value("jdbc:mysql://localhost:3306/task_db"))
                .andExpect(jsonPath("$.propertySources[1].source['server.port']").value("8082"));
    }

    @Test
    void profilesChangeTheValues() throws Exception {
        mvc.perform(get("/auth-service/prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source['spring.datasource.url']")
                        .value("jdbc:mysql://mysql-auth:3306/auth_db"))
                .andExpect(jsonPath("$.propertySources[0].source['spring.datasource.hikari.connection-timeout']")
                        .value("5000"));
    }

    @Test
    void gatewayRoutesPointToTheServicesOfTheProfile() throws Exception {
        mvc.perform(get("/api-gateway/dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source['taskhub.auth-service.url']").value("http://localhost:8081"));
        mvc.perform(get("/api-gateway/prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source['taskhub.task-service.url']").value("http://task-service:8082"));
    }

    @Test
    void theConfigRepoHoldsNoSecrets() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.list(Path.of("../config-repo"))) {
            offenders = files.flatMap(f -> {
                try {
                    return Files.readAllLines(f).stream()
                            .filter(l -> !l.startsWith("#"))
                            .filter(l -> l.toLowerCase().matches(".*(password|secret|token).*=.*"))
                            .map(l -> f.getFileName() + ": " + l);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        }
        assertThat(offenders).isEmpty();
    }
}
