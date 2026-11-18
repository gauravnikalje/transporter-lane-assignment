package com.assignment.transporter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A transporter can quote a lane once, so (transporter, lane) is the key. */
@Embeddable
public record QuoteId(@Column(name = "transporter_id") Long transporterId, @Column(name = "lane_id") Long laneId) {
}
