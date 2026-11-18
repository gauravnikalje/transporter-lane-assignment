package com.assignment.transporter.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A route that needs a transporter")
public record LaneDto(
        @NotNull @Positive @Schema(example = "1") Long id,
        @NotBlank @Size(max = 100) @Schema(example = "Mumbai") String origin,
        @NotBlank @Size(max = 100) @Schema(example = "Delhi") String destination) {
}
