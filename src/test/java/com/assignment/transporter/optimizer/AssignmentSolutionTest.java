package com.assignment.transporter.optimizer;

import java.util.List;
import java.util.Map;

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
}
