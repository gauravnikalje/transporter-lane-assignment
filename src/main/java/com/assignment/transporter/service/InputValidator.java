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
import com.assignment.transporter.service.exception.InputValidationException;

/**
 * Rules that need the whole payload, beyond the field-level checks on the DTOs: unique ids, quotes that point at
 * real lanes, no repeated quotes, and size caps.
 */
@Component
public class InputValidator {

    private static final int MAX_MESSAGES = 20;

    private final TransporterProperties.Limits limits;

    public InputValidator(TransporterProperties properties) {
        this.limits = properties.limits();
    }

    public void validate(InputRequest request) {
        List<String> errors = new ArrayList<>();
        checkSizes(request, errors);
        if (!errors.isEmpty()) {
            throw new InputValidationException(errors);
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

    private void checkSizes(InputRequest request, List<String> errors) {
        if (request.lanes().size() > limits.maxLanes()) {
            errors.add("At most " + limits.maxLanes() + " lanes are accepted, got " + request.lanes().size());
        }
        if (request.transporters().size() > limits.maxTransporters()) {
            errors.add("At most " + limits.maxTransporters() + " transporters are accepted, got "
                    + request.transporters().size());
        }
        long cells = (long) request.lanes().size() * request.transporters().size();
        if (cells > limits.maxMatrixCells()) {
            errors.add("Lanes x transporters must be at most " + limits.maxMatrixCells() + ", got "
                    + request.lanes().size() + " x " + request.transporters().size() + " = " + cells);
        }
        long quotes = request.transporters().stream().mapToLong(t -> t.laneQuotes().size()).sum();
        if (quotes > limits.maxQuotes()) {
            errors.add("At most " + limits.maxQuotes() + " quotes are accepted in total, got " + quotes);
        }
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
