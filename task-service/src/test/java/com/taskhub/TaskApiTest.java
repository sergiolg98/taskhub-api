package com.taskhub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void crudLifecycle() throws Exception {
        String auth = TestTokens.luis();
        String location = mvc.perform(post("/tasks").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Preparar clase\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.ownerId").value(2))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/tasks").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(hasItem("Preparar clase")));
        mvc.perform(put(location).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\",\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Nuevo"));
        mvc.perform(patch(location + "/status").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(delete(location).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get(location).header("Authorization", auth)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mvc.perform(post("/tasks").header("Authorization", TestTokens.luis())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
