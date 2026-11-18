package com.assignment.transporter.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;
import com.assignment.transporter.repository.LaneRepository;
import com.assignment.transporter.repository.QuoteRepository;
import com.assignment.transporter.repository.TransporterRepository;

/**
 * Keeps the one current input. Writing replaces everything in one transaction; reading returns a consistent
 * copy.
 *
 * <p>A read-write lock keeps a reader from seeing half of a replacement (the three tables are read in separate
 * statements) and keeps two replacements from colliding on primary keys. It is enough because this app runs as a
 * single instance on an in-memory database; with a shared database this would become a database-level lock.
 */
@Service
public class InputDataStore {

    private final LaneRepository lanes;
    private final TransporterRepository transporters;
    private final QuoteRepository quotes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InputDataStore(LaneRepository lanes, TransporterRepository transporters, QuoteRepository quotes) {
        this.lanes = lanes;
        this.transporters = transporters;
        this.quotes = quotes;
    }

    @Transactional
    public void replace(List<LaneEntity> newLanes, List<TransporterEntity> newTransporters,
                        List<QuoteEntity> newQuotes) {
        lock.writeLock().lock();
        try {
            quotes.deleteAllInBatch();
            transporters.deleteAllInBatch();
            lanes.deleteAllInBatch();
            lanes.saveAll(newLanes);
            transporters.saveAll(newTransporters);
            quotes.saveAll(newQuotes);
            quotes.flush();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredInput> load() {
        lock.readLock().lock();
        try {
            List<LaneEntity> storedLanes = lanes.findAll(Sort.by("id"));
            if (storedLanes.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new StoredInput(storedLanes, transporters.findAll(Sort.by("id")), quotes.findAll()));
        } finally {
            lock.readLock().unlock();
        }
    }
}
