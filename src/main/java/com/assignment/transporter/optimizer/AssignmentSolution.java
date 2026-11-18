package com.assignment.transporter.optimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.assignment.transporter.optimizer.AssignmentProblem.NO_QUOTE;

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
     * The assignment produced by a chosen set of transporters. Every lane goes to a chosen transporter whose quote
     * is the cheapest among the chosen ones, so the total cost is fixed by the set.
     *
     * <p>When several chosen transporters quote the same lowest price for a lane, the lanes are shared out to use
     * <b>as many different transporters as possible</b> (a maximum matching between transporters and the lanes
     * they tie on), preferring lower transporter ids. Without ties each lane simply goes to its cheapest transporter.
     *
     * <p>Empty when some lane has no quote from any chosen transporter.
     */
    public static Optional<AssignmentSolution> fromChosen(AssignmentProblem problem, int[] chosenTransporterIndices) {
        // Index order equals id order, so sorted indices are sorted ids.
        int[] chosen = chosenTransporterIndices.clone();
        Arrays.sort(chosen);
        int lanes = problem.laneCount();

        long[] cheapest = new long[lanes];
        Arrays.fill(cheapest, NO_QUOTE);
        for (int transporter : chosen) {
            for (int lane = 0; lane < lanes; lane++) {
                cheapest[lane] = Math.min(cheapest[lane], problem.cost(transporter, lane));
            }
        }
        long total = 0;
        for (long cost : cheapest) {
            if (cost == NO_QUOTE) {
                return Optional.empty();
            }
            total = Math.addExact(total, cost);
        }

        // Default owner: the lowest-id chosen transporter among those tied for the lane.
        int[] owner = new int[lanes];
        boolean anyTie = false;
        int[] tied = new int[lanes];
        for (int transporter : chosen) {
            for (int lane = 0; lane < lanes; lane++) {
                if (problem.cost(transporter, lane) == cheapest[lane] && tied[lane]++ == 0) {
                    owner[lane] = transporter;
                }
            }
        }
        for (int count : tied) {
            anyTie |= count > 1;
        }
        if (anyTie) {
            spreadAcrossTransporters(problem, chosen, cheapest, owner);
        }

        Map<Long, Long> transporterByLane = new TreeMap<>();
        TreeSet<Long> used = new TreeSet<>();
        for (int lane = 0; lane < lanes; lane++) {
            long transporterId = problem.transporterId(owner[lane]);
            transporterByLane.put(problem.laneId(lane), transporterId);
            used.add(transporterId);
        }
        return Optional.of(new AssignmentSolution(total, transporterByLane, new ArrayList<>(used)));
    }

    /**
     * Maximum matching of transporters to distinct lanes they tie on (augmenting paths, transporters tried in
     * ascending id order, which also picks the lowest ids among all maximum matchings). Matched lanes go to their
     * matched transporter; every other lane keeps its lowest-id tied transporter, which is always one already
     * used, otherwise the matching could have been extended.
     */
    private static void spreadAcrossTransporters(AssignmentProblem problem, int[] chosen, long[] cheapest, int[] owner) {
        int lanes = cheapest.length;
        int[][] tiedLanes = new int[chosen.length][];
        for (int p = 0; p < chosen.length; p++) {
            int[] buffer = new int[lanes];
            int size = 0;
            for (int lane = 0; lane < lanes; lane++) {
                if (problem.cost(chosen[p], lane) == cheapest[lane]) {
                    buffer[size++] = lane;
                }
            }
            tiedLanes[p] = Arrays.copyOf(buffer, size);
        }

        int[] matchedTransporter = new int[lanes];
        Arrays.fill(matchedTransporter, -1);
        int[] visitedStamp = new int[lanes];
        for (int p = 0; p < chosen.length; p++) {
            augment(p, tiedLanes, matchedTransporter, visitedStamp, p + 1);
        }
        for (int lane = 0; lane < lanes; lane++) {
            if (matchedTransporter[lane] >= 0) {
                owner[lane] = chosen[matchedTransporter[lane]];
            }
        }
    }

    private static boolean augment(int transporter, int[][] tiedLanes, int[] matchedTransporter, int[] visitedStamp,
                                   int stamp) {
        for (int lane : tiedLanes[transporter]) {
            if (matchedTransporter[lane] < 0) {
                matchedTransporter[lane] = transporter;
                return true;
            }
        }
        for (int lane : tiedLanes[transporter]) {
            if (visitedStamp[lane] == stamp) {
                continue;
            }
            visitedStamp[lane] = stamp;
            if (augment(matchedTransporter[lane], tiedLanes, matchedTransporter, visitedStamp, stamp)) {
                matchedTransporter[lane] = transporter;
                return true;
            }
        }
        return false;
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
