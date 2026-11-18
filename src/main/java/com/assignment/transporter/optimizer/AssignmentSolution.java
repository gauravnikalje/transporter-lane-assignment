package com.assignment.transporter.optimizer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One complete assignment: which transporter serves each lane, the total cost, and the transporters used.
 *
 * @param totalCost            sum of the assigned quotes
 * @param transporterByLane    lane id to transporter id, in ascending lane id order
 * @param selectedTransporters ids of the transporters that serve at least one lane, ascending
 */
public record AssignmentSolution(long totalCost, Map<Long, Long> transporterByLane, List<Long> selectedTransporters) {

    /**
     * The assignment produced by a chosen set of transporters: each lane goes to the cheapest chosen transporter
     * (equal quotes go to the lower id). Empty when some lane has no quote from any chosen transporter.
     */
    public static Optional<AssignmentSolution> fromChosen(AssignmentProblem problem, int[] chosenTransporterIndices) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Lower total cost wins; then more transporters used; then the lexicographically smaller list of ids. */
    public boolean isBetterThan(AssignmentSolution other) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
