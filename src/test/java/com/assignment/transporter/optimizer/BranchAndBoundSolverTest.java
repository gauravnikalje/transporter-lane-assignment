package com.assignment.transporter.optimizer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.assignment.transporter.optimizer.SolverResult.Status;

import static com.assignment.transporter.optimizer.ProblemFixtures.assertValid;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefMainExample;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase1;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase2;
import static com.assignment.transporter.optimizer.ProblemFixtures.dataFile;
import static com.assignment.transporter.optimizer.ProblemFixtures.ids;
import static com.assignment.transporter.optimizer.ProblemFixtures.lanesTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BranchAndBoundSolverTest {

    private static final long DEFAULT_BUDGET = 5_000_000;

    private final AssignmentSolver solver = new BranchAndBoundSolver(new GreedyLocalSearchSolver(), DEFAULT_BUDGET);

    @ParameterizedTest(name = "Test Case 1, max {0} -> cost {1}, transporters {2}")
    @CsvSource({
            "1, 96940, 4",
            "2, 62938, 1|4",
            "3, 60139, 1|4|5",
            "4, 59896, 1|3|4|5",
            "7, 59896, 1|3|4|5"
    })
    void solvesBriefTestCase1(int maxTransporters, long expectedCost, String expectedTransporters) {
        SolverResult result = solver.solve(briefTestCase1(), maxTransporters);

        assertThat(result.status()).isEqualTo(Status.OPTIMAL);
        assertThat(result.solution().totalCost()).isEqualTo(expectedCost);
        assertThat(result.solution().selectedTransporters()).isEqualTo(ids(expectedTransporters));
        assertValid(briefTestCase1(), maxTransporters, result.solution());
    }

    @Test
    void beatsTheExampleAnswerPrintedInTheBrief() {
        // The brief prints 75000 (its listed lanes actually sum to 69001, using four transporters).
        AssignmentSolution solution = solver.solve(briefTestCase1(), 3).solution();

        assertThat(solution.totalCost()).isEqualTo(60139).isLessThan(69001);
        assertThat(solution.transporterByLane()).isEqualTo(lanesTo(1, 5, 2, 1, 3, 4, 4, 4, 5, 4));
    }

    @ParameterizedTest(name = "Test Case 2, max {0} is infeasible")
    @ValueSource(ints = {1, 2})
    void reportsBriefTestCase2AsInfeasibleBelowThreeTransporters(int maxTransporters) {
        assertThat(solver.solve(briefTestCase2(), maxTransporters).status()).isEqualTo(Status.INFEASIBLE);
    }

    @Test
    void solvesBriefTestCase2WithThreeTransporters() {
        AssignmentSolution solution = solver.solve(briefTestCase2(), 3).solution();

        assertThat(solution.totalCost()).isEqualTo(7500);
        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L, 3L);
        assertThat(solution.transporterByLane()).isEqualTo(lanesTo(1, 1, 2, 1, 3, 2, 4, 2, 5, 3));
    }

    @Test
    void usesAFourthTransporterInTestCase2WhenItLowersTheCost() {
        AssignmentSolution solution = solver.solve(briefTestCase2(), 4).solution();

        assertThat(solution.totalCost()).isEqualTo(7450);
        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L, 3L, 4L);
        assertThat(solution.transporterByLane()).containsEntry(4L, 4L).containsEntry(1L, 1L);
    }

    @ParameterizedTest(name = "brief main example, max {0} is infeasible")
    @ValueSource(ints = {3, 4})
    void reportsTheBriefMainExampleAsInfeasibleBelowFiveTransporters(int maxTransporters) {
        assertThat(solver.solve(briefMainExample(), maxTransporters).status()).isEqualTo(Status.INFEASIBLE);
    }

    @ParameterizedTest(name = "brief main example, max {0} -> 48000")
    @ValueSource(ints = {5, 7})
    void solvesTheBriefMainExampleWithFiveTransporters(int maxTransporters) {
        AssignmentSolution solution = solver.solve(briefMainExample(), maxTransporters).solution();

        assertThat(solution.totalCost()).isEqualTo(48000);
        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @ParameterizedTest(name = "data file, max {0} -> cost {1}")
    @CsvSource({
            "3, 134876, 1|4|6",
            "4, 127844, 1|3|4|7",
            "5, 125045, 1|3|4|5|7"
    })
    void solvesTheProvidedDataFile(int maxTransporters, long expectedCost, String expectedTransporters) {
        AssignmentProblem problem = dataFile(Integer.MAX_VALUE);

        SolverResult result = solver.solve(problem, maxTransporters);

        assertThat(problem.laneCount()).isEqualTo(9);
        assertThat(result.status()).isEqualTo(Status.OPTIMAL);
        assertThat(result.solution().totalCost()).isEqualTo(expectedCost);
        assertThat(result.solution().selectedTransporters()).isEqualTo(ids(expectedTransporters));
    }

    @Test
    void agreesWithBruteForceOnRandomProblems() {
        Random random = new Random(20261118L);
        BruteForceSolver oracle = new BruteForceSolver();

        for (int i = 0; i < 600; i++) {
            AssignmentProblem problem = ProblemFixtures.random(random, 9, 10, 20);
            int maxTransporters = 1 + random.nextInt(problem.transporterCount());

            SolverResult expected = oracle.solve(problem, maxTransporters);
            SolverResult actual = solver.solve(problem, maxTransporters);

            assertThat(actual.status()).as("problem %d", i).isEqualTo(expected.status());
            if (expected.solution() != null) {
                // Same cost and same number of transporters used. Which of several equally good id sets is
                // returned depends on search order, so the ids themselves are not compared.
                assertThat(actual.solution().totalCost()).as("problem %d", i).isEqualTo(expected.solution().totalCost());
                assertThat(actual.solution().selectedTransporters()).as("problem %d", i)
                        .hasSameSizeAs(expected.solution().selectedTransporters());
                assertValid(problem, maxTransporters, actual.solution());
            }
        }
    }

    @Test
    void usesMoreTransportersWhenQuotesTie() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2)
                .quote(1, 1, 100).quote(1, 2, 100).quote(2, 1, 100).quote(2, 2, 100).build();

        AssignmentSolution solution = solver.solve(problem, 2).solution();

        assertThat(solution.totalCost()).isEqualTo(200);
        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L);
        assertThat(solution.transporterByLane()).isEqualTo(lanesTo(1, 1, 2, 2));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void finishesQuicklyAndProvesOptimalWhenEveryQuoteIsEqual() {
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long transporter = 1; transporter <= 15; transporter++) {
            for (long lane = 1; lane <= 12; lane++) {
                builder.lane(lane).quote(transporter, lane, 100);
            }
        }

        SolverResult result = solver.solve(builder.build(), 4);

        assertThat(result.status()).isEqualTo(Status.OPTIMAL);
        assertThat(result.solution().totalCost()).isEqualTo(1_200);
        assertThat(result.solution().selectedTransporters()).hasSize(4);
        assertThat(result.nodesExplored()).isLessThan(1_000);
    }

    @Test
    void hugeQuotesNeitherOverflowNorChangeTheAnswer() {
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long lane = 1; lane <= 3; lane++) {
            builder.lane(lane).quote(1, lane, 1).quote(2, lane, AssignmentProblem.MAX_COST);
        }

        SolverResult result = solver.solve(builder.build(), 2);

        assertThat(result.status()).isEqualTo(Status.OPTIMAL);
        assertThat(result.solution().totalCost()).isEqualTo(3);
        assertThat(result.solution().selectedTransporters()).containsExactly(1L);
    }

    @Test
    void returnsTheSeedSolutionWhenTheNodeBudgetRunsOut() {
        AssignmentProblem problem = dataFile(Integer.MAX_VALUE);
        AssignmentSolver tinyBudget = new BranchAndBoundSolver(new GreedyLocalSearchSolver(), 1);

        SolverResult result = tinyBudget.solve(problem, 3);

        assertThat(result.status()).isEqualTo(Status.FEASIBLE);
        assertValid(problem, 3, result.solution());
        assertThat(result.solution().totalCost()).isGreaterThanOrEqualTo(134876);
    }

    @Test
    void reportsUnknownWhenTheBudgetRunsOutWithoutAnySolution() {
        AssignmentSolver noSeed = (problem, max) -> SolverResult.unknown(0);
        AssignmentSolver tinyBudget = new BranchAndBoundSolver(noSeed, 1);

        assertThat(tinyBudget.solve(dataFile(Integer.MAX_VALUE), 3).status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void provesInfeasibilityWithoutSearchingWhenRequiredTransportersExceedTheLimit() {
        // In Test Case 2, lanes 2, 3 and 5 each have a single bidder, so three transporters are required.
        SolverResult result = solver.solve(briefTestCase2(), 2);

        assertThat(result.status()).isEqualTo(Status.INFEASIBLE);
        assertThat(result.nodesExplored()).isZero();
    }

    @Test
    void reportsALaneWithoutQuotesAsInfeasible() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2).quote(1, 1, 10).build();

        assertThat(solver.solve(problem, 5).status()).isEqualTo(Status.INFEASIBLE);
    }

    @Test
    void rejectsALimitBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> solver.solve(briefTestCase1(), 0));
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void handlesALargerProblemWithinTheBudget() {
        // 200 lanes, 60 transporters, about half the quotes present; transporter 1 quotes every lane.
        Random random = new Random(99);
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (int lane = 1; lane <= 200; lane++) {
            builder.lane(lane);
            for (int transporter = 1; transporter <= 60; transporter++) {
                if (transporter == 1 || random.nextDouble() < 0.5) {
                    builder.quote(transporter, lane, 1_000 + random.nextInt(50_000));
                }
            }
        }
        AssignmentProblem problem = builder.build();

        SolverResult result = solver.solve(problem, 6);
        SolverResult greedy = new GreedyLocalSearchSolver().solve(problem, 6);

        assertThat(result.status()).isIn(Status.OPTIMAL, Status.FEASIBLE);
        assertValid(problem, 6, result.solution());
        assertThat(result.solution().totalCost()).isLessThanOrEqualTo(greedy.solution().totalCost());
    }

    // ---------------------------------------------------------------- ties must not make the search run away

    /** The production settings (see application.properties), so these tests see what a request would see. */
    private final AssignmentSolver productionSolver =
            new BranchAndBoundSolver(new GreedyLocalSearchSolver(10_000_000), 25_000_000);

    @Test
    void agreesWithBruteForceWhenQuotesTieHeavily() {
        Random random = new Random(808);
        BruteForceSolver oracle = new BruteForceSolver();

        for (int i = 0; i < 600; i++) {
            AssignmentProblem problem = ProblemFixtures.random(random, 8, 8, 2); // costs of 1 or 2: almost all ties
            int maxTransporters = 1 + random.nextInt(problem.transporterCount());

            SolverResult expected = oracle.solve(problem, maxTransporters);
            SolverResult actual = solver.solve(problem, maxTransporters);

            assertThat(actual.status()).as("problem %d", i).isEqualTo(expected.status());
            if (expected.solution() != null) {
                assertThat(actual.solution().totalCost()).as("problem %d", i).isEqualTo(expected.solution().totalCost());
                assertThat(actual.solution().selectedTransporters()).as("problem %d", i)
                        .hasSameSizeAs(expected.solution().selectedTransporters());
                assertValid(problem, maxTransporters, actual.solution());
            }
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void fiveLanesAndTwoHundredTransportersAllQuotingTheSamePriceIsInstant() {
        // Found in review: this took 45 s once ties were shared out, with the answer obvious from the start.
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long lane = 1; lane <= 5; lane++) {
            builder.lane(lane);
            for (long transporter = 1; transporter <= 200; transporter++) {
                builder.quote(transporter, lane, 5_000);
            }
        }

        SolverResult result = productionSolver.solve(builder.build(), 200);

        assertThat(result.status()).isEqualTo(Status.OPTIMAL);
        assertThat(result.solution().totalCost()).isEqualTo(25_000);
        assertThat(result.solution().selectedTransporters()).hasSize(5);
        assertThat(result.nodesExplored()).isLessThan(1_000);
    }

    @ParameterizedTest(name = "50 lanes x 200 transporters, 5 distinct prices, limit {0}")
    @ValueSource(ints = {100, 200})
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void fewDistinctPricesOnAWideTableStayWithinTheBudget(int limit) {
        Random random = new Random(limit);
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long lane = 1; lane <= 50; lane++) {
            builder.lane(lane);
            for (long transporter = 1; transporter <= 200; transporter++) {
                builder.quote(transporter, lane, 1_000L * (1 + random.nextInt(5)));
            }
        }
        AssignmentProblem problem = builder.build();
        long started = System.nanoTime();

        SolverResult result = productionSolver.solve(problem, limit);

        System.out.println("5 prices, 50x200, limit " + limit + ": " + result.status() + " in "
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
        assertThat(result.solutionIfAny()).isPresent();
        assertValid(problem, limit, result.solution());
        assertThat(result.solution().totalCost()).isEqualTo(50_000); // every lane gets a price-1000 transporter
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void aStarOfTiedTransportersFindsTheTwoThatCanBeUsedAndStops() {
        // T1 quotes all 50 lanes; T2..T200 quote only lane 1; every quote is 1000. Only two can ever be used.
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long lane = 1; lane <= 50; lane++) {
            builder.lane(lane).quote(1, lane, 1_000);
        }
        for (long transporter = 2; transporter <= 200; transporter++) {
            builder.quote(transporter, 1, 1_000);
        }

        SolverResult result = productionSolver.solve(builder.build(), 100);

        assertThat(result.status()).isEqualTo(Status.OPTIMAL);
        assertThat(result.solution().totalCost()).isEqualTo(50_000);
        assertThat(result.solution().selectedTransporters()).hasSize(2).contains(1L);
    }
}
