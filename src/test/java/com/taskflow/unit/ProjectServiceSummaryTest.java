package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceSummaryTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void resumen_proyectoConTareas_calculaCorrectamente() throws com.taskflow.exception.TaskValidationException {
        Project p = new Project(2L, "App Móvil", "desc", 1L, LocalDate.now());
        Task t1 = new Task(10L, "T1x", null, TaskStatus.TODO, Priority.LOW, 2L, null, null);
        Task t2 = new Task(11L, "T2x", null, TaskStatus.IN_PROGRESS, Priority.MED, 2L, null, LocalDate.now().minusDays(1)); // vencida
        Task t3 = new Task(12L, "T3x", null, TaskStatus.IN_PROGRESS, Priority.HIGH, 2L, null, null);
        Task t4 = new Task(13L, "T4x", null, TaskStatus.DONE, Priority.LOW, 2L, null, LocalDate.now().minusDays(5));

        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1, t2, t3, t4));

        ProjectSummaryResponse res = projectService.resumen(p);

        assertThat(res.projectId()).isEqualTo(2L);
        assertThat(res.projectName()).isEqualTo("App Móvil");
        assertThat(res.totalTasks()).isEqualTo(4);
        assertThat(res.byStatus().get(TaskStatus.TODO)).isEqualTo(1);
        assertThat(res.byStatus().get(TaskStatus.IN_PROGRESS)).isEqualTo(2);
        assertThat(res.byStatus().get(TaskStatus.DONE)).isEqualTo(1);
        assertThat(res.overdue()).isEqualTo(1);
    }

    @Test
    void resumen_proyectoSinTareas_devuelveCeros() throws com.taskflow.exception.TaskValidationException {
        Project p = new Project(3L, "Sin Tareas", "desc", 1L, LocalDate.now());
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        ProjectSummaryResponse res = projectService.resumen(p);

        assertThat(res.totalTasks()).isEqualTo(0);
        assertThat(res.byStatus().get(TaskStatus.TODO)).isEqualTo(0);
        assertThat(res.byStatus().get(TaskStatus.IN_PROGRESS)).isEqualTo(0);
        assertThat(res.byStatus().get(TaskStatus.DONE)).isEqualTo(0);
        assertThat(res.overdue()).isEqualTo(0);
    }
}
