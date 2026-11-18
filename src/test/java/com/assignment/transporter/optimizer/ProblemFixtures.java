package com.assignment.transporter.optimizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Problems taken from the assignment brief and its data file, plus random ones for property tests. */
final class ProblemFixtures {

    private static final String DATA_FILE = "/data/transporter_assignment_data.csv";

    private ProblemFixtures() {
    }

    /**
     * The provided data file: "Lane N" rows, columns T1..T7. Its first five lanes are exactly the brief's
     * Test Case 1.
     */
    static AssignmentProblem dataFile(int laneCount) {
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        try (InputStream in = Objects.requireNonNull(ProblemFixtures.class.getResourceAsStream(DATA_FILE));
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String[] header = reader.readLine().trim().split(",");
            String line;
            int lanesRead = 0;
            while ((line = reader.readLine()) != null && lanesRead < laneCount) {
                if (line.isBlank()) {
                    continue;
                }
                String[] cells = line.trim().split(",");
                long laneId = Long.parseLong(cells[0].replace("Lane", "").trim());
                builder.lane(laneId);
                for (int column = 1; column < cells.length; column++) {
                    long transporterId = Long.parseLong(header[column].trim().substring(1));
                    builder.quote(transporterId, laneId, Long.parseLong(cells[column].trim()));
                }
                lanesRead++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return builder.build();
    }

    /** Test Case 1 of the brief (5 lanes, 7 transporters, every transporter quotes every lane). */
    static AssignmentProblem briefTestCase1() {
        return dataFile(5);
    }

    /** Test Case 2 of the brief: transporters X=1, Y=2, Z=3, W=4 with sparse quotes. */
    static AssignmentProblem briefTestCase2() {
        return AssignmentProblem.builder()
                .lane(1).lane(2).lane(3).lane(4).lane(5)
                .quote(1, 1, 1000).quote(1, 2, 1500)
                .quote(2, 3, 2000).quote(2, 4, 1800)
                .quote(3, 5, 1200)
                .quote(4, 1, 1100).quote(4, 4, 1750)
                .build();
    }

    /** The first request body in the brief: 10 lanes, 7 transporters, two lanes each. */
    static AssignmentProblem briefMainExample() {
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long lane = 1; lane <= 10; lane++) {
            builder.lane(lane);
        }
        return builder
                .quote(1, 1, 5000).quote(1, 2, 6000)
                .quote(2, 3, 7000).quote(2, 4, 4000)
                .quote(3, 5, 3000).quote(3, 6, 2000)
                .quote(4, 7, 8000).quote(4, 8, 5000)
                .quote(5, 9, 4500).quote(5, 10, 3500)
                .quote(6, 1, 5500).quote(6, 2, 6200)
                .quote(7, 3, 7500).quote(7, 4, 4500)
                .build();
    }

    /**
     * A random problem. Small cost ranges create many equal quotes, which exercises the tie-break rules.
     * About one in five problems leaves a lane without any quote.
     */
    static AssignmentProblem random(Random random, int maxTransporters, int maxLanes, int maxCost) {
        int transporters = 1 + random.nextInt(maxTransporters);
        int lanes = 1 + random.nextInt(maxLanes);
        double density = 0.2 + 0.8 * random.nextDouble();
        boolean coverEveryLane = random.nextDouble() < 0.8;

        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (int t = 1; t <= transporters; t++) {
            builder.transporter(t);
        }
        for (int l = 1; l <= lanes; l++) {
            builder.lane(l);
            boolean quoted = false;
            for (int t = 1; t <= transporters; t++) {
                if (random.nextDouble() < density) {
                    builder.quote(t, l, 1 + random.nextInt(maxCost));
                    quoted = true;
                }
            }
            if (!quoted && coverEveryLane) {
                builder.quote(1 + random.nextInt(transporters), l, 1 + random.nextInt(maxCost));
            }
        }
        return builder.build();
    }

    static Map<Long, Long> lanesTo(long... laneThenTransporter) {
        Map<Long, Long> map = new HashMap<>();
        for (int i = 0; i < laneThenTransporter.length; i += 2) {
            map.put(laneThenTransporter[i], laneThenTransporter[i + 1]);
        }
        return map;
    }

    static List<Long> ids(String pipeSeparated) {
        List<Long> ids = new ArrayList<>();
        for (String id : pipeSeparated.split("\\|")) {
            ids.add(Long.parseLong(id.trim()));
        }
        return ids;
    }

    /** Checks a solution against the problem's rules, independently of any solver. */
    static void assertValid(AssignmentProblem problem, int maxTransporters, AssignmentSolution solution) {
        Map<Long, Integer> transporterIndexById = new HashMap<>();
        for (int t = 0; t < problem.transporterCount(); t++) {
            transporterIndexById.put(problem.transporterId(t), t);
        }
        assertThat(solution.transporterByLane()).hasSize(problem.laneCount());
        long total = 0;
        for (int l = 0; l < problem.laneCount(); l++) {
            Long transporterId = solution.transporterByLane().get(problem.laneId(l));
            assertThat(transporterId).as("lane %d is assigned", problem.laneId(l)).isNotNull();
            int t = transporterIndexById.get(transporterId);
            assertThat(problem.hasQuote(t, l)).as("transporter %d quoted lane %d", transporterId, problem.laneId(l))
                    .isTrue();
            total += problem.cost(t, l);
        }
        assertThat(solution.totalCost()).isEqualTo(total);
        assertThat(solution.selectedTransporters())
                .isSorted()
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(solution.transporterByLane().values().stream().distinct().toList());
        assertThat(solution.selectedTransporters()).hasSizeLessThanOrEqualTo(maxTransporters);
    }
}
