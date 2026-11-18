package com.assignment.transporter.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 * statements) and keeps two replacements from colliding on primary keys. The transaction runs <em>inside</em> the
 * lock (a {@link TransactionTemplate}, not {@code @Transactional}): with the annotation the transaction would
 * commit after the lock was released, and the next writer could start before the previous commit. It is enough
 * for one instance on an in-memory database; with a shared database this would become a database-level lock.
 */
@Service
public class InputDataStore {

    private final LaneRepository lanes;
    private final TransporterRepository transporters;
    private final QuoteRepository quotes;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readTransaction;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InputDataStore(LaneRepository lanes, TransporterRepository transporters, QuoteRepository quotes,
                          PlatformTransactionManager transactionManager) {
        this.lanes = lanes;
        this.transporters = transporters;
        this.quotes = quotes;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
    }

    public void replace(List<LaneEntity> newLanes, List<TransporterEntity> newTransporters,
                        List<QuoteEntity> newQuotes) {
        lock.writeLock().lock();
        try {
            writeTransaction.executeWithoutResult(status -> {
                quotes.deleteAllInBatch();
                transporters.deleteAllInBatch();
                lanes.deleteAllInBatch();
                lanes.saveAll(newLanes);
                transporters.saveAll(newTransporters);
                quotes.saveAll(newQuotes);
                quotes.flush();
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<StoredInput> load() {
        lock.readLock().lock();
        try {
            return readTransaction.execute(status -> {
                List<LaneEntity> storedLanes = lanes.findAll(Sort.by("id"));
                if (storedLanes.isEmpty()) {
                    return Optional.<StoredInput>empty();
                }
                return Optional.of(
                        new StoredInput(storedLanes, transporters.findAll(Sort.by("id")), quotes.findAll()));
            });
        } finally {
            lock.readLock().unlock();
        }
    }
}
