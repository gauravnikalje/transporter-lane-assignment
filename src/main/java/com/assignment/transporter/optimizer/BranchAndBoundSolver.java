package com.assignment.transporter.optimizer;

/**
 * Exact solver: depth-first branch and bound over which transporters to choose.
 */
public final class BranchAndBoundSolver implements AssignmentSolver {

    private final AssignmentSolver seedSolver;
    private final long nodeBudget;

    public BranchAndBoundSolver(AssignmentSolver seedSolver, long nodeBudget) {
        this.seedSolver = seedSolver;
        this.nodeBudget = nodeBudget;
    }

    @Override
    public SolverResult solve(AssignmentProblem problem, int maxTransporters) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
