package com.assignment.transporter.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"status", "totalCost", "assignments", "selectedTransporters", "optimal"})
public record AssignmentResponse(
        @Schema(example = "success") String status,
        @Schema(description = "Sum of the chosen quotes, whole rupees", example = "60139") long totalCost,
        List<LaneAssignment> assignments,
        @Schema(description = "Ids of the transporters used, ascending", example = "[1, 4, 5]")
        List<Long> selectedTransporters,
        @Schema(description = "true when the search finished and no cheaper answer exists. false only when the "
                + "search budget ran out on a very large input and this is the best answer found.")
        boolean optimal) {

    public record LaneAssignment(@Schema(example = "1") long laneId, @Schema(example = "5") long transporterId) {
    }
}
