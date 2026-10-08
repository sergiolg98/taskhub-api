package com.taskhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class TestTokens {

    private TestTokens() {
    }

    static String login(MockMvc mvc, String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(body).get("token").asText();
    }

    static String luis(MockMvc mvc) throws Exception {
        return "Bearer " + login(mvc, "luis@taskhub.com", "def456");
    }

    static String ana(MockMvc mvc) throws Exception {
        return "Bearer " + login(mvc, "ana@taskhub.com", "abc123");
    }
}
