package com.assignment.transporter.optimizer;

import java.util.Random;

import org.junit.jupiter.api.Test;

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
