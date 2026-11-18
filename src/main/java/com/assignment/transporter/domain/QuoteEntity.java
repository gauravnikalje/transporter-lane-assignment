package com.assignment.transporter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "quote")
public class QuoteEntity extends NewAwareEntity<QuoteId> {

    @EmbeddedId
    private QuoteId id;

    /** Whole rupees. */
    @Column(nullable = false)
    private long amount;

    protected QuoteEntity() {
    }

    public QuoteEntity(Long transporterId, Long laneId, long amount) {
        this.id = new QuoteId(transporterId, laneId);
        this.amount = amount;
    }

    @Override
    public QuoteId getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }
}
