package com.assignment.transporter.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assignment.transporter.api.dto.AssignmentRequest;
import com.assignment.transporter.api.dto.AssignmentResponse;
import com.assignment.transporter.api.dto.AssignmentResponse.LaneAssignment;
import com.assignment.transporter.api.dto.InputRequest;
import com.assignment.transporter.api.dto.InputResponse;
import com.assignment.transporter.api.error.ErrorResponse;
import com.assignment.transporter.service.AssignmentResult;
import com.assignment.transporter.service.AssignmentService;
import com.assignment.transporter.service.TransporterInputService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/v1/transporters", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Transporter assignment")
public class TransporterController {

    private final TransporterInputService inputService;
    private final AssignmentService assignmentService;

    public TransporterController(TransporterInputService inputService, AssignmentService assignmentService) {
        this.inputService = inputService;
        this.assignmentService = assignmentService;
    }

    @Operation(
            summary = "Submit lanes and transporter quotes",
            description = "Stores the data and replaces anything submitted before. Pick an example from the "
                    + "drop-down to run the brief's cases.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InputRequest.class),
                    examples = {
                            @ExampleObject(name = "Brief Test Case 1 (5 lanes, 7 transporters)",
                                    value = ApiExamples.TEST_CASE_1),
                            @ExampleObject(name = "Brief Test Case 2 (sparse quotes)",
                                    value = ApiExamples.TEST_CASE_2),
                            @ExampleObject(name = "Provided data file (9 lanes, 7 transporters)",
                                    value = ApiExamples.DATA_FILE_9_LANES),
                            @ExampleObject(name = "Brief's first example (10 lanes, needs 5 transporters)",
                                    value = ApiExamples.BRIEF_MAIN_EXAMPLE)})))
    @ApiResponse(responseCode = "200", description = "Saved")
    @ApiResponse(responseCode = "400", description = "Invalid input: bad JSON, missing fields, duplicate ids, "
            + "a quote for an unknown lane", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/input", consumes = MediaType.APPLICATION_JSON_VALUE)
    public InputResponse submitInput(@Valid @RequestBody InputRequest request) {
        inputService.save(request);
        return InputResponse.saved();
    }

    @Operation(
            summary = "Get the cheapest assignment using at most N transporters",
            description = "Every lane is assigned to one transporter that quoted it. The total cost is the lowest "
                    + "possible; on equal cost, the answer using more transporters wins.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AssignmentRequest.class),
                    examples = {
                            @ExampleObject(name = "Limit 3", value = ApiExamples.LIMIT_3),
                            @ExampleObject(name = "Limit 2", value = ApiExamples.LIMIT_2),
                            @ExampleObject(name = "Limit 4", value = ApiExamples.LIMIT_4)})))
    @ApiResponse(responseCode = "200", description = "Cheapest assignment")
    @ApiResponse(responseCode = "400", description = "maxTransporters missing or below 1",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "No input submitted yet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "No set of at most N transporters covers every lane",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/assignment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssignmentResponse assign(@Valid @RequestBody AssignmentRequest request) {
        AssignmentResult result = assignmentService.assign(request.maxTransporters());
        return new AssignmentResponse(
                "success",
                result.solution().totalCost(),
                result.solution().transporterByLane().entrySet().stream()
                        .map(entry -> new LaneAssignment(entry.getKey(), entry.getValue()))
                        .toList(),
                result.solution().selectedTransporters(),
                result.optimal());
    }
}
