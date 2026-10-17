package com.taskflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * TaskAssigneeUpdateRequest — DTO para PATCH /tasks/{id}/assignee.
 */
public record TaskAssigneeUpdateRequest(
        @NotNull
        @Positive
        Long assigneeId
) {
}