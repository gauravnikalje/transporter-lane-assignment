package com.assignment.transporter.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;
import com.assignment.transporter.repository.LaneRepository;
import com.assignment.transporter.repository.QuoteRepository;
import com.assignment.transporter.repository.TransporterRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InputDataStoreTest {

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

    @Test
    void loadsNothingBeforeAnyInput() {
        assertThat(store.load()).isEmpty();
    }

    @Test
    void loadsWhatWasStoredInIdOrder() {
        store.replace(
                List.of(new LaneEntity(2L, "C", "D"), new LaneEntity(1L, "A", "B")),
                List.of(new TransporterEntity(5L, "Five"), new TransporterEntity(3L, "Three")),
                List.of(new QuoteEntity(5L, 1L, 100), new QuoteEntity(3L, 2L, 200)));

        StoredInput input = store.load().orElseThrow();

        assertThat(input.lanes()).extracting(LaneEntity::getId).containsExactly(1L, 2L);
        assertThat(input.lanes().get(0).getOrigin()).isEqualTo("A");
        assertThat(input.transporters()).extracting(TransporterEntity::getId).containsExactly(3L, 5L);
        assertThat(input.quotes()).extracting(QuoteEntity::getAmount).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void replacingKeepsOnlyTheNewDataEvenWhenIdsAreReused() {
        store.replace(List.of(new LaneEntity(1L, "Old", "Old")), List.of(new TransporterEntity(1L, "Old")),
                List.of(new QuoteEntity(1L, 1L, 111)));

        store.replace(List.of(new LaneEntity(1L, "New", "New"), new LaneEntity(2L, "New2", "New2")),
                List.of(new TransporterEntity(1L, "New")), List.of(new QuoteEntity(1L, 2L, 222)));

        StoredInput input = store.load().orElseThrow();
        assertThat(input.lanes()).hasSize(2);
        assertThat(input.lanes().get(0).getOrigin()).isEqualTo("New");
        assertThat(input.quotes()).singleElement().satisfies(quote -> {
            assertThat(quote.getId().laneId()).isEqualTo(2L);
            assertThat(quote.getAmount()).isEqualTo(222);
        });
    }

    @Test
    void loadedEntitiesAreNotTreatedAsNew() {
        store.replace(List.of(new LaneEntity(1L, "A", "B")), List.of(new TransporterEntity(1L, "T")), List.of());

        assertThat(store.load().orElseThrow().lanes().get(0).isNew()).isFalse();
        assertThat(new LaneEntity(9L, "A", "B").isNew()).isTrue();
    }
}
