package com.assignment.transporter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.QuoteId;

public interface QuoteRepository extends JpaRepository<QuoteEntity, QuoteId> {
}
