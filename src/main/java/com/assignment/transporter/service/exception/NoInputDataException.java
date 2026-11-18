package com.assignment.transporter.service.exception;

/** An assignment was requested before any input was submitted. */
public class NoInputDataException extends RuntimeException {

    public NoInputDataException() {
        super("No input data has been submitted yet. Call POST /api/v1/transporters/input first.");
    }
}
