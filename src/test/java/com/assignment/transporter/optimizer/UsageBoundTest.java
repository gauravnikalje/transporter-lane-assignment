package com.assignment.transporter.optimizer;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase1;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase2;
import static org.assertj.core.api.Assertions.assertThat;

class UsageBoundTest {

    @Test
    void isTheLaneCountWhenEnoughTransportersQuoteEveryLane() {
        assertThat(UsageBound.maxDistinctTransporters(briefTestCase1())).isEqualTo(5);
    }

    @Test
    void isTheTransporterCountWhenLanesOutnumberThem() {
        assertThat(UsageBound.maxDistinctTransporters(briefTestCase2())).isEqualTo(4);
    }

    @Test
    void countsAStarShapeAsTwoNotAsTheNumberOfTransporters() {
        // T1 quotes every lane; T2..T50 quote only lane 1. At most T1 and one other can ever own a lane.
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (long lane = 1; lane <= 20; lane++) {
            builder.lane(lane).quote(1, lane, 1_000);
        }
        for (long transporter = 2; transporter <= 50; transporter++) {
            builder.quote(transporter, 1, 1_000);
        }

        assertThat(UsageBound.maxDistinctTransporters(builder.build())).isEqualTo(2);
    }

    @Test
    void ignoresTransportersThatQuotedNothing() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).transporter(7).quote(1, 1, 10).build();

        assertThat(UsageBound.maxDistinctTransporters(problem)).isEqualTo(1);
    }

    @Test
    void neverUnderestimatesWhatAnyAnswerUses() {
        Random random = new Random(21);
        BruteForceSolver oracle = new BruteForceSolver();
        for (int i = 0; i < 400; i++) {
            AssignmentProblem problem = ProblemFixtures.random(random, 8, 8, 3);
            int bound = UsageBound.maxDistinctTransporters(problem);
            for (int limit = 1; limit <= problem.transporterCount(); limit++) {
                oracle.solve(problem, limit).solutionIfAny().ifPresent(solution ->
                        assertThat(solution.selectedTransporters().size()).isLessThanOrEqualTo(bound));
            }
        }
    }
}
