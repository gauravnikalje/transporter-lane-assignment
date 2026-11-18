package com.assignment.transporter.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.assignment.transporter.api.dto.InputRequest;
import com.assignment.transporter.api.dto.LaneDto;
import com.assignment.transporter.api.dto.LaneQuoteDto;
import com.assignment.transporter.api.dto.TransporterDto;
import com.assignment.transporter.config.TransporterProperties;
import com.assignment.transporter.service.exception.FieldValidationException;
import com.assignment.transporter.service.exception.InputValidationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Validates a submitted input in three steps, cheapest first, so a hostile body cannot make the expensive steps run
 * on millions of elements:
 * <ol>
 *   <li><b>Sizes</b>: how many lanes, transporters and quotes, counted without looking inside them.</li>
 *   <li><b>Fields</b>: the Bean Validation rules on the DTOs (required, not blank, above zero, at most 1 trillion).
 *       Only reached when the sizes are within the caps, so it sees a bounded number of elements.</li>
 *   <li><b>Whole payload</b>: unique ids, quotes that point at real lanes, no repeated quotes.</li>
 * </ol>
 * Every list of messages is capped, so the response stays small however bad the body is.
 */
@Component
public class InputValidator {

    private static final int MAX_MESSAGES = 20;

    private final TransporterProperties.Limits limits;
    private final Validator fieldValidator;

    public InputValidator(TransporterProperties properties, Validator fieldValidator) {
        this.limits = properties.limits();
        this.fieldValidator = fieldValidator;
    }

    public void validate(InputRequest request) {
        List<String> errors = new ArrayList<>();
        checkSizes(request, errors);
        if (!errors.isEmpty()) {
            throw new InputValidationException(errors);
        }

        List<String> fieldErrors = fieldErrors(request);
        if (!fieldErrors.isEmpty()) {
            throw new FieldValidationException(fieldErrors);
        }

        Set<Long> laneIds = new HashSet<>();
        for (int i = 0; i < request.lanes().size(); i++) {
            LaneDto lane = request.lanes().get(i);
            if (!laneIds.add(lane.id())) {
                add(errors, "lanes[" + i + "]: lane id " + lane.id() + " is used more than once");
            }
        }

        Set<Long> transporterIds = new HashSet<>();
        for (int t = 0; t < request.transporters().size(); t++) {
            TransporterDto transporter = request.transporters().get(t);
            if (!transporterIds.add(transporter.id())) {
                add(errors, "transporters[" + t + "]: transporter id " + transporter.id() + " is used more than once");
            }
            Set<Long> quotedLanes = new HashSet<>();
            for (int q = 0; q < transporter.laneQuotes().size(); q++) {
                LaneQuoteDto quote = transporter.laneQuotes().get(q);
                String where = "transporters[" + t + "].laneQuotes[" + q + "]";
                if (!laneIds.contains(quote.laneId())) {
                    add(errors, where + ": lane " + quote.laneId() + " is not in the lanes list");
                }
                if (!quotedLanes.add(quote.laneId())) {
                    add(errors, where + ": transporter " + transporter.id() + " quoted lane " + quote.laneId()
                            + " more than once");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new InputValidationException(errors);
        }
    }

    /** Counts only; safe on null lists and null entries, which the field step reports later. */
    private void checkSizes(InputRequest request, List<String> errors) {
        int lanes = sizeOf(request.lanes());
        int transporters = sizeOf(request.transporters());
        long quotes = 0;
        if (request.transporters() != null) {
            for (TransporterDto transporter : request.transporters()) {
                quotes += transporter == null ? 0 : sizeOf(transporter.laneQuotes());
            }
        }

        if (lanes > limits.maxLanes()) {
            errors.add("At most " + limits.maxLanes() + " lanes are accepted, got " + lanes);
        }
        if (transporters > limits.maxTransporters()) {
            errors.add("At most " + limits.maxTransporters() + " transporters are accepted, got " + transporters);
        }
        long cells = (long) lanes * transporters;
        if (cells > limits.maxMatrixCells()) {
            errors.add("Lanes x transporters must be at most " + limits.maxMatrixCells() + ", got "
                    + lanes + " x " + transporters + " = " + cells);
        }
        if (quotes > limits.maxQuotes()) {
            errors.add("At most " + limits.maxQuotes() + " quotes are accepted in total, got " + quotes);
        }
    }

    private List<String> fieldErrors(InputRequest request) {
        List<String> all = new ArrayList<>();
        for (ConstraintViolation<InputRequest> violation : fieldValidator.validate(request)) {
            // "lanes[0].<list element>" is Hibernate Validator's name for a null entry of the list
            String path = violation.getPropertyPath().toString().replace(".<list element>", "");
            all.add(path + " " + violation.getMessage());
        }
        all.sort(null);
        if (all.size() <= MAX_MESSAGES) {
            return all;
        }
        List<String> capped = new ArrayList<>(all.subList(0, MAX_MESSAGES));
        capped.add("... and " + (all.size() - MAX_MESSAGES) + " more");
        return capped;
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    /** Stops after a screenful so a huge broken payload does not produce a huge response. */
    private static void add(List<String> errors, String message) {
        if (errors.size() < MAX_MESSAGES) {
            errors.add(message);
        } else if (errors.size() == MAX_MESSAGES) {
            errors.add("... and more");
        }
    }
}
