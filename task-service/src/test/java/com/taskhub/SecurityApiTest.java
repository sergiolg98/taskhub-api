package com.taskhub;

import com.taskhub.common.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        mvc.perform(get("/tasks").header("Authorization", TestTokens.luis() + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminTasksAreOnlyForAdmins() throws Exception {
        mvc.perform(get("/admin/tasks").header("Authorization", TestTokens.luis()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/tasks").header("Authorization", TestTokens.ana()))
                .andExpect(status().isOk());
    }

    // auth-service is not involved: a valid signature is enough. Verifying that the owner still exists comes in class 10.
    @Test
    void aTokenOfAUserThisServiceHasNeverSeenCreatesTasks() throws Exception {
        String ghost = TestTokens.token(999L, "ghost@taskhub.com", "USER");

        mvc.perform(post("/tasks").header("Authorization", ghost)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(999));
    }

    @Test
    void otherUsersTaskIs404ButAdminCanSeeIt() throws Exception {
        String location = mvc.perform(post("/tasks").header("Authorization", TestTokens.luis())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Privada\"}"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location).header("Authorization", TestTokens.ana())).andExpect(status().isOk());
        mvc.perform(get(location).header("Authorization", TestTokens.eva())).andExpect(status().isNotFound());
        mvc.perform(delete(location).header("Authorization", TestTokens.eva())).andExpect(status().isNotFound());
    }

    @Test
    void errorsGeneratedBySpringUseTheSameFormat() throws Exception {
        String luis = TestTokens.luis();
        mvc.perform(get("/nope").header("Authorization", luis))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Not Found"));
        mvc.perform(get("/tasks/abc").header("Authorization", luis))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void jwtFilterLivesOnlyInsideTheSecurityChain() {
        // A Filter bean is also registered by Spring Boot as a servlet filter, so it would run twice.
        assertThat(context.getBeanNamesForType(JwtAuthenticationFilter.class)).isEmpty();
    }
}
