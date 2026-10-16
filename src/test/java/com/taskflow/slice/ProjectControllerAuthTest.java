package com.taskflow.slice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProjectControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getResumen_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/projects/2/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
