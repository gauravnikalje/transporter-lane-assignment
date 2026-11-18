package com.assignment.transporter.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a transporter charges for one lane")
public record LaneQuoteDto(
        @NotNull @Positive @Schema(description = "Id of a lane in the same request", example = "1") Long laneId,
        @NotNull @Positive @Schema(description = "Whole rupees, above zero", example = "20835") Long quote) {
}
