package com.assignment.transporter.api.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/** Every error response has this shape. */
@JsonPropertyOrder({"status", "code", "message", "errors"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(example = "error") String status,
        @Schema(description = "Stable machine-readable reason", example = "VALIDATION_FAILED") String code,
        @Schema(example = "The request is not valid.") String message,
        @Schema(description = "One line per problem, when there are several") List<String> errors) {

    public static ErrorResponse of(String code, String message, List<String> errors) {
        return new ErrorResponse("error", code, message, errors == null ? List.of() : errors);
    }

    public static ErrorResponse of(String code, String message) {
        return of(code, message, List.of());
    }
}
