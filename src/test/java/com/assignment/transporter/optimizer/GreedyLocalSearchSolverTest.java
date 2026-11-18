package com.assignment.transporter.optimizer;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.assignment.transporter.optimizer.SolverResult.Status;

import static com.assignment.transporter.optimizer.ProblemFixtures.assertValid;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase1;
import static com.assignment.transporter.optimizer.ProblemFixtures.briefTestCase2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GreedyLocalSearchSolverTest {

    private final GreedyLocalSearchSolver solver = new GreedyLocalSearchSolver();

    @Test
    void solvesTestCase2WhereTheRequiredTransportersAreForced() {
        SolverResult result = solver.solve(briefTestCase2(), 3);

        assertThat(result.status()).isEqualTo(Status.FEASIBLE);
        assertThat(result.solution().totalCost()).isEqualTo(7500);
    }

    @Test
    void neverClaimsOptimality() {
        assertThat(solver.solve(briefTestCase1(), 3).status()).isEqualTo(Status.FEASIBLE);
    }

    @Test
    void provesInfeasibilityWhenRequiredTransportersExceedTheLimit() {
        assertThat(solver.solve(briefTestCase2(), 2).status()).isEqualTo(Status.INFEASIBLE);
    }

    @Test
    void provesInfeasibilityWhenALaneHasNoQuote() {
        AssignmentProblem problem = AssignmentProblem.builder().lane(1).lane(2).quote(1, 1, 10).build();

        assertThat(solver.solve(problem, 3).status()).isEqualTo(Status.INFEASIBLE);
    }

    @Test
    void stopsWhenItsWorkBudgetIsSpentAndNeverReturnsAnInvalidAnswer() {
        AssignmentProblem problem = ProblemFixtures.dataFile(Integer.MAX_VALUE);

        SolverResult result = new GreedyLocalSearchSolver(1).solve(problem, 3);

        assertThat(result.status()).isIn(Status.FEASIBLE, Status.UNKNOWN);
        result.solutionIfAny().ifPresent(solution -> assertValid(problem, 3, solution));
    }

    @Test
    void rejectsAZeroWorkBudget() {
        assertThatIllegalArgumentException().isThrownBy(() -> new GreedyLocalSearchSolver(0));
    }

    @Test
    void rejectsALimitBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> solver.solve(briefTestCase1(), 0));
    }

    @Test
    void alwaysReturnsValidAssignmentsThatAreNeverBetterThanTheOptimum() {
        Random random = new Random(42);
        BruteForceSolver oracle = new BruteForceSolver();

        for (int i = 0; i < 400; i++) {
            AssignmentProblem problem = ProblemFixtures.random(random, 9, 10, 50);
            int maxTransporters = 1 + random.nextInt(problem.transporterCount());

            SolverResult best = oracle.solve(problem, maxTransporters);
            SolverResult result = solver.solve(problem, maxTransporters);

            if (best.status() == Status.INFEASIBLE) {
                assertThat(result.status()).as("problem %d", i).isIn(Status.INFEASIBLE, Status.UNKNOWN);
            } else if (result.status() == Status.FEASIBLE) {
                assertValid(problem, maxTransporters, result.solution());
                assertThat(result.solution().totalCost()).isGreaterThanOrEqualTo(best.solution().totalCost());
            } else {
                assertThat(result.status()).as("problem %d", i).isEqualTo(Status.UNKNOWN);
            }
        }
    }
}
