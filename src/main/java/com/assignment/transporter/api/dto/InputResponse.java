package com.assignment.transporter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InputResponse(
        @Schema(example = "success") String status,
        @Schema(example = "Input data saved successfully.") String message) {

    public static InputResponse saved() {
        return new InputResponse("success", "Input data saved successfully.");
    }
}
