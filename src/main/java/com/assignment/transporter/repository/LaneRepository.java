package com.assignment.transporter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assignment.transporter.domain.LaneEntity;

public interface LaneRepository extends JpaRepository<LaneEntity, Long> {
}
