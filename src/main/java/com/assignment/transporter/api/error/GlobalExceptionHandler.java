package com.assignment.transporter.api.error;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.assignment.transporter.service.exception.InfeasibleAssignmentException;
import com.assignment.transporter.service.exception.InputValidationException;
import com.assignment.transporter.service.exception.NoInputDataException;
import com.assignment.transporter.service.exception.SearchLimitReachedException;

import tools.jackson.core.JacksonException;

/**
 * Turns every failure into the same {@link ErrorResponse} shape. Extends Spring's handler so framework errors
 * (wrong method, wrong content type, unknown URL) also use it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InputValidationException.class)
    ResponseEntity<ErrorResponse> inputInvalid(InputValidationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("INVALID_INPUT", ex.getMessage(), ex.errors()));
    }

    @ExceptionHandler(NoInputDataException.class)
    ResponseEntity<ErrorResponse> noInput(NoInputDataException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("NO_INPUT_DATA", ex.getMessage()));
    }

    @ExceptionHandler(InfeasibleAssignmentException.class)
    ResponseEntity<ErrorResponse> infeasible(InfeasibleAssignmentException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ErrorResponse.of("NO_FEASIBLE_ASSIGNMENT", ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(SearchLimitReachedException.class)
    ResponseEntity<ErrorResponse> searchLimit(SearchLimitReachedException ex) {
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.of("SEARCH_LIMIT_REACHED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Something went wrong on the server."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();
        return body(HttpStatus.BAD_REQUEST, ErrorResponse.of("VALIDATION_FAILED", "The request is not valid.", errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String where = ex.getCause() instanceof JacksonException jackson ? pathOf(jackson) : "";
        String message = where.isEmpty()
                ? "The request body is missing or is not valid JSON."
                : "The request body has a wrong value or type at '" + where + "'.";
        return body(HttpStatus.BAD_REQUEST, ErrorResponse.of("MALFORMED_REQUEST", message));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message = ex instanceof org.springframework.web.ErrorResponse framework
                ? framework.getBody().getDetail() : "The request could not be processed.";
        String code = HttpStatus.resolve(statusCode.value()) != null
                ? HttpStatus.resolve(statusCode.value()).name() : "ERROR";
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(code, message));
    }

    private static ResponseEntity<Object> body(HttpStatus status, ErrorResponse error) {
        return ResponseEntity.status(status).body(error);
    }

    /** "transporters[0].laneQuotes[1].quote", from Jackson's path to the bad value. */
    private static String pathOf(JacksonException exception) {
        StringBuilder path = new StringBuilder();
        List<JacksonException.Reference> references = new ArrayList<>(exception.getPath());
        for (JacksonException.Reference reference : references) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }
}
