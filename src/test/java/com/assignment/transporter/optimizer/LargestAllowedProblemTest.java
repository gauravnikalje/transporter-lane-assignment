package com.assignment.transporter.optimizer;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static com.assignment.transporter.optimizer.ProblemFixtures.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The production solver settings on inputs at the edge of what the API accepts (2,000 lanes, 200 transporters,
 * 10,000 quotes, 200,000 table cells), so a request can never hang or run out of time.
 */
class LargestAllowedProblemTest {

    /** Same as the defaults of {@code transporter.solver.work-budget} and {@code seed-work-budget}. */
    private final AssignmentSolver solver = new BranchAndBoundSolver(new GreedyLocalSearchSolver(10_000_000), 25_000_000);

    /** Every lane is quoted by one guaranteed transporter plus about {@code density} of the others. */
    private static AssignmentProblem table(int lanes, int transporters, double density, long seed) {
        Random random = new Random(seed);
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (int lane = 1; lane <= lanes; lane++) {
            builder.lane(lane);
            int guaranteed = 1 + random.nextInt(transporters);
            for (int transporter = 1; transporter <= transporters; transporter++) {
                if (transporter == guaranteed || random.nextDouble() < density) {
                    builder.quote(transporter, lane, 1_000 + random.nextInt(90_000));
                }
            }
        }
        return builder.build();
    }

    private SolverResult timed(String label, AssignmentProblem problem, int limit) {
        long started = System.nanoTime();
        SolverResult result = solver.solve(problem, limit);
        System.out.println(label + ": " + result.status() + " in "
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms, nodes " + result.nodesExplored());
        return result;
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void answers1000LanesBy200TransportersWithAHugeLimit() {
        AssignmentProblem problem = table(1_000, 200, 0.05, 1);

        SolverResult result = timed("1000x200 limit 200", problem, 200);

        assertThat(result.solutionIfAny()).isPresent();
        assertValid(problem, 200, result.solution());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void answers1000LanesBy200TransportersWithATightLimit() {
        AssignmentProblem problem = table(1_000, 200, 0.05, 2);

        SolverResult result = timed("1000x200 limit 40", problem, 40);

        // Whether 40 transporters can cover every lane is a matter of luck here; what matters is no hang and no
        // invalid answer.
        result.solutionIfAny().ifPresent(solution -> assertValid(problem, 40, solution));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void answers2000LanesBy100Transporters() {
        AssignmentProblem problem = table(2_000, 100, 0.05, 3);

        SolverResult result = timed("2000x100 limit 60", problem, 60);

        assertThat(result.solutionIfAny()).isPresent();
        assertValid(problem, 60, result.solution());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void answersADenseTableWithASmallLimit() {
        AssignmentProblem problem = table(100, 100, 1.0, 4);

        SolverResult result = timed("100x100 fully quoted limit 10", problem, 10);

        assertThat(result.solutionIfAny()).isPresent();
        assertValid(problem, 10, result.solution());
    }
}
