package com.assignment.transporter.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;
import com.assignment.transporter.repository.LaneRepository;
import com.assignment.transporter.repository.QuoteRepository;
import com.assignment.transporter.repository.TransporterRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many threads replace and read the input at once. Both datasets reuse the same transporter ids (so two
 * overlapping replacements would collide on primary keys) but use different lane ids (so a reader that saw half of
 * a replacement would find quotes pointing at lanes that are not there).
 */
@SpringBootTest
class InputDataStoreConcurrencyTest {

    private static final int WRITERS = 8;
    private static final int WRITES_EACH = 100;
    private static final int READERS = 4;
    private static final int READS_EACH = 150;
    private static final int LANES = 30;
    private static final int TRANSPORTERS = 5;

    @Autowired
    InputDataStore store;
    @Autowired
    LaneRepository lanes;
    @Autowired
    TransporterRepository transporters;
    @Autowired
    QuoteRepository quotes;

    @BeforeEach
    void emptyDatabase() {
        quotes.deleteAllInBatch();
        transporters.deleteAllInBatch();
        lanes.deleteAllInBatch();
    }

    private void storeDataset(int variant) {
        long firstLane = variant == 0 ? 1 : 1_001;
        List<LaneEntity> newLanes = new ArrayList<>();
        List<TransporterEntity> newTransporters = new ArrayList<>();
        List<QuoteEntity> newQuotes = new ArrayList<>();
        for (long transporter = 1; transporter <= TRANSPORTERS; transporter++) {
            newTransporters.add(new TransporterEntity(transporter, "T" + transporter));
        }
        for (long lane = firstLane; lane < firstLane + LANES; lane++) {
            newLanes.add(new LaneEntity(lane, "From " + lane, "To " + lane));
            for (long transporter = 1; transporter <= TRANSPORTERS; transporter++) {
                newQuotes.add(new QuoteEntity(transporter, lane, 100 + lane + transporter));
            }
        }
        store.replace(newLanes, newTransporters, newQuotes);
    }

    private void assertConsistent(StoredInput input) {
        Set<Long> laneIds = new HashSet<>();
        input.lanes().forEach(lane -> laneIds.add(lane.getId()));
        assertThat(laneIds).hasSize(LANES);
        assertThat(laneIds.stream().allMatch(id -> id <= LANES) || laneIds.stream().allMatch(id -> id > 1_000))
                .as("lanes come from one dataset only: %s", laneIds).isTrue();
        assertThat(input.transporters()).hasSize(TRANSPORTERS);
        assertThat(input.quotes()).hasSize(LANES * TRANSPORTERS);
        input.quotes().forEach(quote -> assertThat(laneIds).contains(quote.getId().laneId()));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void overlappingReplacementsAndReadsNeverFailAndNeverSeeAMixture() throws Exception {
        storeDataset(0);
        ExecutorService pool = Executors.newFixedThreadPool(WRITERS + READERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int w = 0; w < WRITERS; w++) {
            int writer = w;
            futures.add(pool.submit((Callable<Void>) () -> {
                go.await();
                for (int i = 0; i < WRITES_EACH; i++) {
                    storeDataset((writer + i) % 2);
                }
                return null;
            }));
        }
        for (int r = 0; r < READERS; r++) {
            futures.add(pool.submit((Callable<Void>) () -> {
                go.await();
                for (int i = 0; i < READS_EACH; i++) {
                    assertConsistent(store.load().orElseThrow());
                }
                return null;
            }));
        }
        go.countDown();

        List<Throwable> failures = new ArrayList<>();
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                failures.add(e.getCause() == null ? e : e.getCause());
            }
        }
        pool.shutdownNow();

        assertThat(failures).as("failures: %s", failures.stream().map(Throwable::toString).distinct().toList()).isEmpty();
    }
}
