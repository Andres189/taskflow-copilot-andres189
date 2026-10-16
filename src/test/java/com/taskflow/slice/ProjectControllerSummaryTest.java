package com.taskflow.slice;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.TaskResponse;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.taskflow.controller.ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProjectControllerSummaryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private com.taskflow.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser // simplifica autenticación en slice test
    void getResumen_proyectoExistente_devuelve200yJson() throws Exception {
        EnumMap<TaskStatus, Integer> byStatus = new EnumMap<>(TaskStatus.class);
        byStatus.put(TaskStatus.TODO, 1);
        byStatus.put(TaskStatus.IN_PROGRESS, 2);
        byStatus.put(TaskStatus.DONE, 1);
        ProjectSummaryResponse resp = new ProjectSummaryResponse(2L, "App Móvil", 4, Map.copyOf(byStatus), 1);
        when(projectService.buscarPorId(2L)).thenReturn(java.util.Optional.of(new com.taskflow.model.Project(2L, "App Móvil", "desc", 1L, java.time.LocalDate.now())));
        when(projectService.resumen(org.mockito.ArgumentMatchers.any())).thenReturn(resp);

        mockMvc.perform(get("/projects/2/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(2))
                .andExpect(jsonPath("$.projectName").value("App Móvil"))
                .andExpect(jsonPath("$.totalTasks").value(4))
                .andExpect(jsonPath("$.byStatus.TODO").value(1))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.byStatus.DONE").value(1))
                .andExpect(jsonPath("$.overdue").value(1));
    }

    @Test
    @WithMockUser
    void getResumen_proyectoInexistente_devuelve404() throws Exception {
        when(projectService.buscarPorId(99L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/projects/99/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
