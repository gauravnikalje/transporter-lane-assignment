package com.assignment.transporter.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.assignment.transporter.optimizer.AssignmentProblem;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a transporter charges for one lane")
public record LaneQuoteDto(
        @NotNull @Positive @Schema(description = "Id of a lane in the same request", example = "1") Long laneId,
        @NotNull @Positive @Max(AssignmentProblem.MAX_COST)
        @Schema(description = "Whole rupees, above zero and at most 1 trillion", example = "20835") Long quote) {
}
