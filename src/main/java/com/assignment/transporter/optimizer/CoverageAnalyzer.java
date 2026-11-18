package com.assignment.transporter.optimizer;

import java.util.OptionalInt;

/**
 * Works out the smallest number of transporters that can cover every lane, used to explain an infeasible limit.
 */
public final class CoverageAnalyzer {

    private final long nodeBudget;

    public CoverageAnalyzer(long nodeBudget) {
        this.nodeBudget = nodeBudget;
    }

    public OptionalInt minimumTransportersToCover(AssignmentProblem problem) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
