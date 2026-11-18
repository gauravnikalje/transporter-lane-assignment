package com.assignment.transporter.optimizer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static com.assignment.transporter.optimizer.ProblemFixtures.briefMainExample;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase1;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase2;
import static org.assertj.core.api.Assertions.assertThat;

class CoverageAnalyzerTest {

    private final CoverageAnalyzer analyzer = new CoverageAnalyzer(1_000_000);

    @Test
    void oneTransporterCoversTestCase1() {
        assertThat(analyzer.minimumTransportersToCover(briefTestCase1())).hasValue(1);
    }

    @Test
    void testCase2NeedsThreeTransporters() {
        assertThat(analyzer.minimumTransportersToCover(briefTestCase2())).hasValue(3);
    }

    @Test
    void theBriefMainExampleNeedsFiveTransporters() {
        assertThat(analyzer.minimumTransportersToCover(briefMainExample())).hasValue(5);
    }

    @Test
    void isEmptyWhenALaneHasNoQuoteAtAll() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2).quote(1, 1, 10).build();

        assertThat(analyzer.minimumTransportersToCover(problem)).isEmpty();
    }

    @Test
    void isEmptyWhenTheBudgetRunsOut() {
        assertThat(new CoverageAnalyzer(1).minimumTransportersToCover(briefMainExample())).isEmpty();
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void stopsInTimeOnAHugeTableWhoseCoverIsHardToFind() {
        // 2,000 lanes x 200 transporters, each lane quoted by only 1 or 2 transporters: a large search tree.
        Random random = new Random(3);
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (int lane = 1; lane <= 2_000; lane++) {
            builder.lane(lane);
            int first = 1 + random.nextInt(200);
            builder.quote(first, lane, 1_000);
            int second = 1 + random.nextInt(200);
            if (second != first) {
                builder.quote(second, lane, 1_000);
            }
        }
        long started = System.nanoTime();

        // Same as the default of transporter.solver.coverage-work-budget.
        new CoverageAnalyzer(20_000_000).minimumTransportersToCover(builder.build());

        System.out.println("coverage analyzer on 2000x200: "
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
    }

    @Test
    void agreesWithTheSmallestFeasibleLimitFromBruteForce() {
        Random random = new Random(5);
        BruteForceSolver oracle = new BruteForceSolver();

        for (int i = 0; i < 200; i++) {
            AssignmentProblem problem = ProblemFixtures.random(random, 8, 8, 10);
            Integer smallest = null;
            for (int k = 1; k <= problem.transporterCount() && smallest == null; k++) {
                if (oracle.solve(problem, k).solutionIfAny().isPresent()) {
                    smallest = k;
                }
            }
            if (smallest == null) {
                assertThat(analyzer.minimumTransportersToCover(problem)).as("problem %d", i).isEmpty();
            } else {
                assertThat(analyzer.minimumTransportersToCover(problem)).as("problem %d", i).hasValue(smallest);
            }
        }
    }
}
