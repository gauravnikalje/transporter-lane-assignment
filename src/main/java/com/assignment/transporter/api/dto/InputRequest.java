package com.assignment.transporter.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of {@code POST /api/v1/transporters/input}. */
@Schema(description = "All lanes, and each transporter's quotes for them. Replaces any earlier input.")
public record InputRequest(
        @NotEmpty(message = "must contain at least one lane") List<@NotNull(message = "must not be null") @Valid LaneDto> lanes,
        @NotEmpty(message = "must contain at least one transporter")
        List<@NotNull(message = "must not be null") @Valid TransporterDto> transporters) {
}
