package com.assignment.transporter.domain;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

/**
 * Entities here have ids that come from the client, not the database. Without this, Spring Data would run a
 * SELECT before every insert to decide between insert and update. Telling it "new" up front lets inserts batch.
 */
@MappedSuperclass
abstract class NewAwareEntity<ID> implements Persistable<ID> {

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
