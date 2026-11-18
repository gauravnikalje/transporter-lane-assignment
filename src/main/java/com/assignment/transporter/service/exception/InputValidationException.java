package com.assignment.transporter.service.exception;

import java.util.List;

/** The input is well-formed JSON but breaks a rule that needs the whole payload to check. */
public class InputValidationException extends RuntimeException {

    private final transient List<String> errors;

    public InputValidationException(List<String> errors) {
        super("The input data is not valid.");
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
