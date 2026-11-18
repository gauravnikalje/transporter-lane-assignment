package com.assignment.transporter.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of {@code POST /api/v1/transporters/assignment}. */
public record AssignmentRequest(
        @NotNull(message = "is required")
        @Min(value = 1, message = "must be at least 1")
        @Schema(description = "Most transporters that may be used. A value above the number of transporters is fine.",
                example = "3")
        Integer maxTransporters) {
}
