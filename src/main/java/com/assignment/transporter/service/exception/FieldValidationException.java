package com.assignment.transporter.service.exception;

import java.util.List;

/** One or more fields are missing, blank, out of range or of the wrong shape (the per-field checks on the DTOs). */
public class FieldValidationException extends RuntimeException {

    private final transient List<String> errors;

    public FieldValidationException(List<String> errors) {
        super("The request is not valid.");
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
