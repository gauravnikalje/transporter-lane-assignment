package com.assignment.transporter.optimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * One complete assignment: which transporter serves each lane, the total cost, and the transporters used.
 *
 * @param totalCost            sum of the assigned quotes
 * @param transporterByLane    lane id to transporter id, in ascending lane id order
 * @param selectedTransporters ids of the transporters that serve at least one lane, ascending
 */
public record AssignmentSolution(long totalCost, Map<Long, Long> transporterByLane, List<Long> selectedTransporters) {

    public AssignmentSolution {
        if (totalCost < 0) {
            throw new IllegalArgumentException("totalCost must not be negative");
        }
        transporterByLane = Collections.unmodifiableMap(new TreeMap<>(transporterByLane));
        selectedTransporters = List.copyOf(selectedTransporters);
    }

    /**
     * The assignment produced by a chosen set of transporters: each lane goes to the cheapest chosen transporter
     * (equal quotes go to the lower id). Empty when some lane has no quote from any chosen transporter.
     */
    public static Optional<AssignmentSolution> fromChosen(AssignmentProblem problem, int[] chosenTransporterIndices) {
        // Index order equals id order, so scanning ascending with a strict '<' gives ties to the lower id.
        int[] chosen = chosenTransporterIndices.clone();
        Arrays.sort(chosen);

        Map<Long, Long> transporterByLane = new TreeMap<>();
        TreeSet<Long> used = new TreeSet<>();
        long total = 0;
        for (int lane = 0; lane < problem.laneCount(); lane++) {
            int owner = -1;
            long cheapest = AssignmentProblem.NO_QUOTE;
            for (int transporter : chosen) {
                long cost = problem.cost(transporter, lane);
                if (cost < cheapest) {
                    cheapest = cost;
                    owner = transporter;
                }
            }
            if (owner < 0) {
                return Optional.empty();
            }
            total = Math.addExact(total, cheapest);
            long ownerId = problem.transporterId(owner);
            transporterByLane.put(problem.laneId(lane), ownerId);
            used.add(ownerId);
        }
        return Optional.of(new AssignmentSolution(total, transporterByLane, new ArrayList<>(used)));
    }

    /** Lower total cost wins; then more transporters used; then the lexicographically smaller list of ids. */
    public boolean isBetterThan(AssignmentSolution other) {
        if (other == null) {
            return true;
        }
        if (totalCost != other.totalCost) {
            return totalCost < other.totalCost;
        }
        int mine = selectedTransporters.size();
        int theirs = other.selectedTransporters.size();
        if (mine != theirs) {
            return mine > theirs;
        }
        for (int i = 0; i < mine; i++) {
            int comparison = Long.compare(selectedTransporters.get(i), other.selectedTransporters.get(i));
            if (comparison != 0) {
                return comparison < 0;
            }
        }
        return false;
    }
}
