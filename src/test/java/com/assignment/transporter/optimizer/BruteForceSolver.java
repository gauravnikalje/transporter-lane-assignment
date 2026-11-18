package com.assignment.transporter.optimizer;

/**
 * Test oracle: tries every set of at most {@code maxTransporters} transporters. Only for tiny problems.
 */
final class BruteForceSolver implements AssignmentSolver {

    @Override
    public SolverResult solve(AssignmentProblem problem, int maxTransporters) {
        AssignmentSolver.requireValidLimit(maxTransporters);
        int n = problem.transporterCount();
        if (n > 20) {
            throw new IllegalArgumentException("Brute force is for at most 20 transporters");
        }
        AssignmentSolution best = null;
        long subsets = 0;
        for (int mask = 1; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) > maxTransporters) {
                continue;
            }
            subsets++;
            int[] chosen = new int[Integer.bitCount(mask)];
            for (int t = 0, i = 0; t < n; t++) {
                if ((mask & (1 << t)) != 0) {
                    chosen[i++] = t;
                }
            }
            AssignmentSolution candidate = AssignmentSolution.fromChosen(problem, chosen).orElse(null);
            if (candidate != null && candidate.isBetterThan(best)) {
                best = candidate;
            }
        }
        return best == null ? SolverResult.infeasible(subsets) : SolverResult.optimal(best, subsets);
    }
}
