package com.assignment.transporter.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A transporter and the lanes it quoted")
public record TransporterDto(
        @NotNull @Positive @Schema(example = "1") Long id,
        @NotBlank @Size(max = 100) @Schema(example = "Transporter T1") String name,
        @NotNull(message = "must not be null (use an empty list for no quotes)")
        List<@NotNull(message = "must not be null") @Valid LaneQuoteDto> laneQuotes) {
}
