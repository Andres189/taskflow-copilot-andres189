package com.taskflow.mapper;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mapea la información agregada de un proyecto hacia ProjectSummaryResponse.
 */
public final class ProjectSummaryMapper {

    private ProjectSummaryMapper() {}

    public static ProjectSummaryResponse aResponse(Project project, List<Task> tareas) {
        EnumMap<TaskStatus, Integer> byStatus = new EnumMap<>(TaskStatus.class);
        // Inicializar siempre las tres claves
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, 0);
        }
        int overdue = 0;
        for (Task t : tareas) {
            TaskStatus s = t.getStatus();
            byStatus.put(s, byStatus.get(s) + 1);
            if (t.estaVencida()) {
                overdue++;
            }
        }
        int total = tareas.size();
        return new ProjectSummaryResponse(project.getId(), project.getName(), total, Map.copyOf(byStatus), overdue);
    }
}
