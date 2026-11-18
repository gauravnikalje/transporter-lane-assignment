package com.assignment.transporter.service;

import java.util.List;

import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;

/** A consistent copy of everything that was last submitted. */
public record StoredInput(List<LaneEntity> lanes, List<TransporterEntity> transporters, List<QuoteEntity> quotes) {
}
