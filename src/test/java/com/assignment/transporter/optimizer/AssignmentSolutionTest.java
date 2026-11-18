package com.assignment.transporter.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase1;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase2;
import static com.assignment.transporter.optimizer.ProblemFixtures.lanesTo;
import static org.assertj.core.api.Assertions.assertThat;

class AssignmentSolutionTest {

    @Test
    void assignsEachLaneToTheCheapestChosenTransporter() {
        // indices 0, 3, 4 are transporters T1, T4, T5
        AssignmentSolution solution = AssignmentSolution.fromChosen(briefTestCase1(), new int[] {0, 3, 4}).orElseThrow();

        assertThat(solution.totalCost()).isEqualTo(60139);
        assertThat(solution.transporterByLane()).isEqualTo(lanesTo(1, 5, 2, 1, 3, 4, 4, 4, 5, 4));
        assertThat(solution.selectedTransporters()).containsExactly(1L, 4L, 5L);
    }

    @Test
    void listsLanesInAscendingOrder() {
        AssignmentSolution solution = AssignmentSolution.fromChosen(briefTestCase1(), new int[] {4, 0, 3}).orElseThrow();

        assertThat(solution.transporterByLane().keySet()).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void givesAnEqualQuoteToTheLowerTransporterId() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).quote(2, 1, 100).quote(1, 1, 100).build();

        AssignmentSolution solution = AssignmentSolution.fromChosen(problem, new int[] {1, 0}).orElseThrow();

        assertThat(solution.transporterByLane()).containsEntry(1L, 1L);
        assertThat(solution.selectedTransporters()).containsExactly(1L);
    }

    @Test
    void leavesOutAChosenTransporterThatWinsNoLane() {
        // T2 (index 1) is never cheaper than T4 on these lanes
        AssignmentSolution solution = AssignmentSolution.fromChosen(briefTestCase1(), new int[] {0, 1, 3, 4}).orElseThrow();

        assertThat(solution.selectedTransporters()).containsExactly(1L, 4L, 5L);
    }

    @Test
    void isEmptyWhenALaneHasNoQuoteFromTheChosenTransporters() {
        // X and Y cannot serve lane 5
        assertThat(AssignmentSolution.fromChosen(briefTestCase2(), new int[] {0, 1})).isEmpty();
    }

    @Test
    void lowerCostIsBetter() {
        AssignmentSolution cheap = new AssignmentSolution(10, Map.of(1L, 1L), List.of(1L));
        AssignmentSolution dear = new AssignmentSolution(11, Map.of(1L, 2L), List.of(1L, 2L));

        assertThat(cheap.isBetterThan(dear)).isTrue();
        assertThat(dear.isBetterThan(cheap)).isFalse();
        assertThat(cheap.isBetterThan(null)).isTrue();
    }

    @Test
    void onEqualCostMoreTransportersIsBetter() {
        AssignmentSolution two = new AssignmentSolution(10, Map.of(1L, 1L, 2L, 2L), List.of(1L, 2L));
        AssignmentSolution one = new AssignmentSolution(10, Map.of(1L, 1L, 2L, 1L), List.of(1L));

        assertThat(two.isBetterThan(one)).isTrue();
        assertThat(one.isBetterThan(two)).isFalse();
    }

    @Test
    void onEqualCostAndCountTheSmallerIdsAreBetter() {
        AssignmentSolution low = new AssignmentSolution(10, Map.of(1L, 1L, 2L, 3L), List.of(1L, 3L));
        AssignmentSolution high = new AssignmentSolution(10, Map.of(1L, 2L, 2L, 3L), List.of(2L, 3L));

        assertThat(low.isBetterThan(high)).isTrue();
        assertThat(high.isBetterThan(low)).isFalse();
        assertThat(low.isBetterThan(low)).isFalse();
    }

    // ---------------------------------------------------------------- ties: use as many transporters as possible

    @Test
    void sharesTiedLanesOutToUseMoreTransporters() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2)
                .quote(1, 1, 100).quote(1, 2, 100).quote(2, 1, 100).quote(2, 2, 100).build();

        AssignmentSolution solution = AssignmentSolution.fromChosen(problem, new int[] {0, 1}).orElseThrow();

        assertThat(solution.totalCost()).isEqualTo(200);
        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L);
        assertThat(solution.transporterByLane()).isEqualTo(lanesTo(1, 1, 2, 2));
    }

    @Test
    void takesALaneBackFromAnEarlierTransporterWhenThatLetsAnotherOneIn() {
        // T1 ties on lanes 1 and 2 but is the only cheapest on lane 2, so T1 must give lane 1 to T2.
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2)
                .quote(1, 1, 100).quote(1, 2, 100).quote(2, 1, 100).quote(2, 2, 150).build();

        AssignmentSolution solution = AssignmentSolution.fromChosen(problem, new int[] {0, 1}).orElseThrow();

        assertThat(solution.totalCost()).isEqualTo(200);
        assertThat(solution.transporterByLane()).isEqualTo(lanesTo(1, 2, 2, 1));
        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L);
    }

    @Test
    void doesNotUseATransporterThatCannotWinAnyLaneOnItsOwn() {
        // One shared lane, two tied transporters: only one can be used, and it is the lower id.
        AssignmentProblem problem = AssignmentProblem.builder().lane(1)
                .quote(2, 1, 100).quote(1, 1, 100).build();

        AssignmentSolution solution = AssignmentSolution.fromChosen(problem, new int[] {0, 1}).orElseThrow();

        assertThat(solution.selectedTransporters()).containsExactly(1L);
    }

    @Test
    void prefersLowerIdsWhenOnlySomeOfTheTiedTransportersCanBeUsed() {
        // Three transporters tie on two lanes: two of them are used, the two lowest ids.
        AssignmentProblem.Builder builder = AssignmentProblem.builder().lane(1).lane(2);
        for (long transporter = 1; transporter <= 3; transporter++) {
            builder.quote(transporter, 1, 100).quote(transporter, 2, 100);
        }

        AssignmentSolution solution = AssignmentSolution.fromChosen(builder.build(), new int[] {2, 1, 0}).orElseThrow();

        assertThat(solution.selectedTransporters()).containsExactly(1L, 2L);
    }

    @Test
    void tieSharingAgreesWithAnExhaustiveSearchOnRandomTinyProblems() {
        Random random = new Random(11);
        int compared = 0;
        for (int i = 0; i < 600; i++) {
            // costs of 1..3 make most lanes tie
            AssignmentProblem problem = ProblemFixtures.random(random, 5, 5, 3);
            int n = problem.transporterCount();
            int mask = 1 + random.nextInt((1 << n) - 1);
            int[] chosen = java.util.stream.IntStream.range(0, n).filter(t -> (mask & (1 << t)) != 0).toArray();

            Optional<AssignmentSolution> actual = AssignmentSolution.fromChosen(problem, chosen);
            Optional<List<Long>> expectedUsed = bestUsedByExhaustiveSearch(problem, chosen);

            assertThat(actual.isPresent()).as("problem %d", i).isEqualTo(expectedUsed.isPresent());
            if (actual.isPresent()) {
                compared++;
                assertThat(actual.get().selectedTransporters()).as("problem %d", i).isEqualTo(expectedUsed.get());
                ProblemFixtures.assertValid(problem, chosen.length, actual.get());
            }
        }
        assertThat(compared).isGreaterThan(300);
    }

    /**
     * Tries every way of giving each lane to one of its cheapest chosen transporters and returns the used-id list
     * with the most transporters, lexicographically smallest among those. Empty when a lane cannot be covered.
     */
    private static Optional<List<Long>> bestUsedByExhaustiveSearch(AssignmentProblem problem, int[] chosen) {
        List<List<Integer>> options = new ArrayList<>();
        for (int lane = 0; lane < problem.laneCount(); lane++) {
            long cheapest = AssignmentProblem.NO_QUOTE;
            for (int t : chosen) {
                cheapest = Math.min(cheapest, problem.cost(t, lane));
            }
            if (cheapest == AssignmentProblem.NO_QUOTE) {
                return Optional.empty();
            }
            List<Integer> tied = new ArrayList<>();
            for (int t : chosen) {
                if (problem.cost(t, lane) == cheapest) {
                    tied.add(t);
                }
            }
            options.add(tied);
        }
        List<List<Long>> best = new ArrayList<>();
        walk(problem, options, 0, new java.util.TreeSet<>(), best);
        return Optional.of(best.get(0));
    }

    private static void walk(AssignmentProblem problem, List<List<Integer>> options, int lane,
                             java.util.TreeSet<Long> used, List<List<Long>> best) {
        if (lane == options.size()) {
            List<Long> candidate = new ArrayList<>(used);
            if (best.isEmpty() || isBetterUsage(candidate, best.get(0))) {
                best.clear();
                best.add(candidate);
            }
            return;
        }
        for (int t : options.get(lane)) {
            long id = problem.transporterId(t);
            boolean added = used.add(id);
            walk(problem, options, lane + 1, used, best);
            if (added) {
                used.remove(id);
            }
        }
    }

    private static boolean isBetterUsage(List<Long> a, List<Long> b) {
        if (a.size() != b.size()) {
            return a.size() > b.size();
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return a.get(i) < b.get(i);
            }
        }
        return false;
    }
}
