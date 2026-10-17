package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.exception.TaskStateException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReasignarTareaControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void patchAssignee_existente_devuelve200Ycampo() throws Exception {
        when(taskService.buscarPorId(4L)).thenReturn(Optional.of(new Task(4L, "T04", "d", TaskStatus.TODO, Priority.MED, 1L, null, null)));
when(taskService.reasignar(any(Task.class), eq(2L))).thenReturn(new Task(4L, "T04", "d", TaskStatus.TODO, Priority.MED, 1L, 2L, null));
        mockMvc.perform(patch("/tasks/4/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(2));
    }
    @Test
    void patchAssignee_tareaDone_devuelve422() throws Exception {
        when(taskService.buscarPorId(2L)).thenReturn(Optional.of(new Task(2L, "T02", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, null)));
        when(taskService.reasignar(any(Task.class), any(Long.class))).thenThrow(new TaskStateException("No se puede reasignar una tarea terminada."));
        mockMvc.perform(patch("/tasks/2/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":3}"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test
    void patchAssignee_proyectoInexistente_devuelve404() throws Exception {
        when(taskService.buscarPorId(99L)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/tasks/99/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
    @Test
    void patchAssignee_cuerpoVacio_devuelve400_y_noLlamaService() throws Exception {
        mockMvc.perform(patch("/tasks/6/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(taskService, never()).reasignar(any(), any());
    }
}
