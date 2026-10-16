package com.taskflow.dto;

import com.taskflow.model.TaskStatus;

import java.util.Map;

/**
 * DTO de salida para GET /projects/{id}/summary
 */
public record ProjectSummaryResponse(Long projectId,
                                     String projectName,
                                     int totalTasks,
                                     Map<TaskStatus, Integer> byStatus,
                                     int overdue) {
}
