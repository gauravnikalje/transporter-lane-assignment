package com.assignment.transporter.optimizer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AssignmentProblemTest {

    @Test
    void storesLanesAndTransportersInAscendingIdOrder() {
        AssignmentProblem problem = AssignmentProblem.builder()
                .lane(30).lane(10).lane(20)
                .quote(9, 10, 5).quote(2, 30, 7)
                .build();

        assertThat(problem.laneCount()).isEqualTo(3);
        assertThat(problem.laneId(0)).isEqualTo(10);
        assertThat(problem.laneId(2)).isEqualTo(30);
        assertThat(problem.transporterId(0)).isEqualTo(2);
        assertThat(problem.transporterId(1)).isEqualTo(9);
        assertThat(problem.cost(1, 0)).isEqualTo(5);
        assertThat(problem.cost(0, 2)).isEqualTo(7);
    }

    @Test
    void marksMissingQuotes() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2).quote(1, 1, 100).build();

        assertThat(problem.hasQuote(0, 0)).isTrue();
        assertThat(problem.hasQuote(0, 1)).isFalse();
        assertThat(problem.cost(0, 1)).isEqualTo(AssignmentProblem.NO_QUOTE);
    }

    @Test
    void keepsTransportersThatQuotedNothing() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).transporter(5).quote(1, 1, 10).build();

        assertThat(problem.transporterCount()).isEqualTo(2);
        assertThat(problem.hasQuote(1, 0)).isFalse();
    }

    @Test
    void rejectsQuoteForUnknownLane() {
        AssignmentProblem.Builder builder = AssignmentProblem.builder().lane(1).quote(1, 99, 10);

        assertThatIllegalArgumentException().isThrownBy(builder::build).withMessageContaining("unknown lane 99");
    }

    @Test
    void rejectsDuplicateQuote() {
        AssignmentProblem.Builder builder = AssignmentProblem.builder().lane(1).quote(1, 1, 10);

        assertThatIllegalArgumentException().isThrownBy(() -> builder.quote(1, 1, 20))
                .withMessageContaining("more than once");
    }

    @Test
    void rejectsNegativeCost() {
        assertThatIllegalArgumentException().isThrownBy(() -> AssignmentProblem.builder().quote(1, 1, -1));
    }

    @Test
    void rejectsACostAboveTheCapSoSumsCannotOverflow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AssignmentProblem.builder().quote(1, 1, AssignmentProblem.MAX_COST + 1))
                .withMessageContaining("between 0 and");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AssignmentProblem.builder().quote(1, 1, Long.MAX_VALUE));
    }

    @Test
    void acceptsACostAtTheCap() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).quote(1, 1, AssignmentProblem.MAX_COST).build();

        assertThat(problem.cost(0, 0)).isEqualTo(AssignmentProblem.MAX_COST);
    }
}
