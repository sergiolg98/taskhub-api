package com.taskhub;

import com.taskhub.common.security.JwtAuthenticationFilter;
import com.taskhub.task.application.port.out.UserLookupPort;
import com.taskhub.task.application.port.out.UserLookupResult;
import com.taskhub.task.domain.model.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
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

    @MockitoBean
    UserLookupPort userLookup;

    @BeforeEach
    void ownersExist() {
        when(userLookup.findById(anyLong())).thenAnswer(inv ->
                new UserLookupResult.Found(new UserSummary(inv.getArgument(0), "Someone", "USER")));
    }

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

    @Test
    void actuatorHealthIsPublicButTheRestIsOnlyForAdmins() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/circuitbreakers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/circuitbreakers").header("Authorization", TestTokens.luis()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/circuitbreakers").header("Authorization", TestTokens.ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreakers.authService.state").value("CLOSED"));
    }

    @Test
    void anOwnerThatNoLongerExistsIs404() throws Exception {
        when(userLookup.findById(999L)).thenReturn(new UserLookupResult.NotFound());
        String ghost = TestTokens.token(999L, "ghost@taskhub.com", "USER");

        mvc.perform(post("/tasks").header("Authorization", ghost)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Owner not found: 999"));
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
