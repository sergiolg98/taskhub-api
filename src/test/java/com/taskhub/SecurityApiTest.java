package com.taskhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        String token = TestTokens.luis(mvc);
        mvc.perform(get("/tasks").header("Authorization", token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userOnAdminEndpointIs403AndAdminIs200() throws Exception {
        mvc.perform(get("/admin/users").header("Authorization", TestTokens.luis(mvc)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").header("Authorization", TestTokens.ana(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void adminTasksAreOnlyForAdmins() throws Exception {
        mvc.perform(get("/admin/tasks").header("Authorization", TestTokens.luis(mvc)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/tasks").header("Authorization", TestTokens.ana(mvc)))
                .andExpect(status().isOk());
    }

    @Test
    void userSummaryExposesOnlyIdNameAndRole() throws Exception {
        mvc.perform(get("/users/2").header("Authorization", TestTokens.luis(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Luis User"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/users/999").header("Authorization", TestTokens.ana(mvc)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/users/2")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerThenLoginAndDuplicateEmail() throws Exception {
        String body = "{\"name\":\"Eva\",\"email\":\"eva@taskhub.com\",\"password\":\"Secret123\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"eva@taskhub.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherUsersTaskIs404ButAdminCanSeeIt() throws Exception {
        String luis = TestTokens.luis(mvc);
        String location = mvc.perform(post("/tasks").header("Authorization", luis)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Privada\"}"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location).header("Authorization", TestTokens.ana(mvc))).andExpect(status().isOk());

        String body = "{\"name\":\"Otro\",\"email\":\"otro@taskhub.com\",\"password\":\"Secret123\"}";
        String other = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String otherToken = "Bearer " + new ObjectMapper().readTree(other).get("token").asText();
        mvc.perform(get(location).header("Authorization", otherToken)).andExpect(status().isNotFound());
        mvc.perform(delete(location).header("Authorization", otherToken)).andExpect(status().isNotFound());
    }
}
