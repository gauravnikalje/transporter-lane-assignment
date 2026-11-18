package com.assignment.transporter.optimizer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static com.assignment.transporter.optimizer.ProblemFixtures.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

/** The production solver settings on inputs at the edge of what the API accepts, so a request can never hang. */
class LargestAllowedProblemTest {

    /** Same as the default {@code transporter.solver.work-budget}. */
    private static final long PRODUCTION_WORK_BUDGET = 50_000_000;

    private final AssignmentSolver solver =
            new BranchAndBoundSolver(new GreedyLocalSearchSolver(), PRODUCTION_WORK_BUDGET);

    /** 2,000 lanes x 1,000 transporters (2 million cells), about 10% of quotes present, every lane quoted. */
    private static AssignmentProblem biggestTable(long seed) {
        Random random = new Random(seed);
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (int lane = 1; lane <= 2_000; lane++) {
            builder.lane(lane);
            int guaranteed = 1 + random.nextInt(1_000);
            for (int transporter = 1; transporter <= 1_000; transporter++) {
                if (transporter == guaranteed || random.nextInt(10) == 0) {
                    builder.quote(transporter, lane, 1_000 + random.nextInt(90_000));
                }
            }
        }
        return builder.build();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void answersTheBiggestTableWithASmallLimitInTime() {
        AssignmentProblem problem = biggestTable(1);
        long started = System.nanoTime();

        SolverResult result = solver.solve(problem, 40);

        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        System.out.println("biggest table, limit 40: " + result.status() + " in " + millis + " ms, nodes "
                + result.nodesExplored());
        assertThat(result.status()).isIn(SolverResult.Status.OPTIMAL, SolverResult.Status.FEASIBLE,
                SolverResult.Status.INFEASIBLE);
        result.solutionIfAny().ifPresent(solution -> assertValid(problem, 40, solution));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void answersTheBiggestTableWithAHugeLimitInTime() {
        AssignmentProblem problem = biggestTable(2);
        long started = System.nanoTime();

        SolverResult result = solver.solve(problem, 1_000);

        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        System.out.println("biggest table, limit 1000: " + result.status() + " in " + millis + " ms, nodes "
                + result.nodesExplored());
        assertThat(result.solutionIfAny()).isPresent();
        assertValid(problem, 1_000, result.solution());
    }
}
