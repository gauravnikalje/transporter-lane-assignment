package com.assignment.transporter.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Works out the smallest number of transporters that can cover every lane (a minimum set cover), so that an
 * infeasible request can say how many transporters it would need.
 *
 * <p>Iterative deepening: try 1, 2, 3... transporters. At each step branch on the uncovered lane with the fewest
 * bidders, since any cover must include one of them. Empty when a lane has no bidder or the work budget runs out.
 *
 * <p>The budget counts lane evaluations (each search step scans every lane), so the time spent is bounded no
 * matter how many lanes there are.
 */
public final class CoverageAnalyzer {

    private final long workBudget;

    /** @param workBudget maximum lane evaluations */
    public CoverageAnalyzer(long workBudget) {
        if (workBudget < 1) {
            throw new IllegalArgumentException("workBudget must be at least 1");
        }
        this.workBudget = workBudget;
    }

    public OptionalInt minimumTransportersToCover(AssignmentProblem problem) {
        int lanes = problem.laneCount();
        if (lanes == 0) {
            return OptionalInt.of(0);
        }
        int[][] bidders = new int[lanes][];
        for (int lane = 0; lane < lanes; lane++) {
            List<Integer> laneBidders = new ArrayList<>();
            for (int t = 0; t < problem.transporterCount(); t++) {
                if (problem.hasQuote(t, lane)) {
                    laneBidders.add(t);
                }
            }
            if (laneBidders.isEmpty()) {
                return OptionalInt.empty();
            }
            bidders[lane] = laneBidders.stream().mapToInt(Integer::intValue).toArray();
        }

        Search search = new Search(problem, bidders);
        for (int size = 1; size <= problem.transporterCount(); size++) {
            Outcome outcome = search.canCover(size, new boolean[lanes], 0);
            if (outcome == Outcome.COVERED) {
                return OptionalInt.of(size);
            }
            if (outcome == Outcome.OUT_OF_BUDGET) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }

    private enum Outcome { COVERED, NOT_COVERED, OUT_OF_BUDGET }

    private final class Search {

        private final AssignmentProblem problem;
        private final int[][] bidders;
        private long work;

        Search(AssignmentProblem problem, int[][] bidders) {
            this.problem = problem;
            this.bidders = bidders;
        }

        Outcome canCover(int picksLeft, boolean[] covered, int coveredCount) {
            // One call scans the lanes to find the hardest one, and its parent copied the array: charge both.
            work += 2L * covered.length;
            if (work > workBudget) {
                return Outcome.OUT_OF_BUDGET;
            }
            if (coveredCount == covered.length) {
                return Outcome.COVERED;
            }
            if (picksLeft == 0) {
                return Outcome.NOT_COVERED;
            }
            int hardestLane = -1;
            for (int lane = 0; lane < covered.length; lane++) {
                if (!covered[lane] && (hardestLane < 0 || bidders[lane].length < bidders[hardestLane].length)) {
                    hardestLane = lane;
                }
            }
            for (int transporter : bidders[hardestLane]) {
                boolean[] next = covered.clone();
                int nextCount = coveredCount;
                for (int lane = 0; lane < next.length; lane++) {
                    if (!next[lane] && problem.hasQuote(transporter, lane)) {
                        next[lane] = true;
                        nextCount++;
                    }
                }
                Outcome outcome = canCover(picksLeft - 1, next, nextCount);
                if (outcome != Outcome.NOT_COVERED) {
                    return outcome;
                }
            }
            return Outcome.NOT_COVERED;
        }
    }
}
