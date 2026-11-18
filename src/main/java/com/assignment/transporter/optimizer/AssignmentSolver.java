package com.assignment.transporter.optimizer;

/**
 * Strategy for choosing at most {@code maxTransporters} transporters and assigning every lane to one of them
 * at the lowest total cost.
 *
 * <p>Rules shared by every implementation (see {@link AssignmentSolution}):
 * <ol>
 *   <li>Every lane must be assigned to a chosen transporter that quoted it.</li>
 *   <li>Each lane goes to the cheapest chosen transporter; an equal quote goes to the lower transporter id.</li>
 *   <li>Lowest total cost wins; on equal cost, more transporters used wins; then the smaller list of ids.</li>
 * </ol>
 */
public interface AssignmentSolver {

    SolverResult solve(AssignmentProblem problem, int maxTransporters);

    static void requireValidLimit(int maxTransporters) {
        if (maxTransporters < 1) {
            throw new IllegalArgumentException("maxTransporters must be at least 1, got " + maxTransporters);
        }
    }
}
