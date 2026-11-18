package com.assignment.transporter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transporter")
public class TransporterEntity extends NewAwareEntity<Long> {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    protected TransporterEntity() {
    }

    public TransporterEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
