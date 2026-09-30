package com.taskhub;

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
class TaskApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void crudLifecycle() throws Exception {
        String location = mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Preparar clase\",\"description\":\"d\",\"ownerId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location)).andExpect(status().isOk());
        mvc.perform(get("/tasks").param("ownerId", "2")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Preparar clase"));
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\",\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Nuevo"));
        mvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankTitleAndUnknownOwner() throws Exception {
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"ownerId\":2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"ownerId\":999}"))
                .andExpect(status().isNotFound());
    }
}
