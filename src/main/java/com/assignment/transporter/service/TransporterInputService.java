package com.assignment.transporter.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.assignment.transporter.api.dto.InputRequest;
import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;

/** Validates the submitted lanes and quotes, then stores them in place of the previous input. */
@Service
public class TransporterInputService {

    private final InputValidator validator;
    private final InputDataStore store;

    public TransporterInputService(InputValidator validator, InputDataStore store) {
        this.validator = validator;
        this.store = store;
    }

    public void save(InputRequest request) {
        validator.validate(request);

        List<LaneEntity> lanes = request.lanes().stream()
                .map(lane -> new LaneEntity(lane.id(), lane.origin().strip(), lane.destination().strip()))
                .toList();
        List<TransporterEntity> transporters = request.transporters().stream()
                .map(transporter -> new TransporterEntity(transporter.id(), transporter.name().strip()))
                .toList();
        List<QuoteEntity> quotes = request.transporters().stream()
                .flatMap(transporter -> transporter.laneQuotes().stream()
                        .map(quote -> new QuoteEntity(transporter.id(), quote.laneId(), quote.quote())))
                .toList();

        store.replace(lanes, transporters, quotes);
    }
}
