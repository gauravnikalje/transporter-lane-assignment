package com.assignment.transporter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assignment.transporter.domain.TransporterEntity;

public interface TransporterRepository extends JpaRepository<TransporterEntity, Long> {
}
