package com.assignment.transporter.optimizer;

import java.util.Arrays;

/**
 * An upper bound on how many different transporters any answer can use.
 *
 * <p>A used transporter owns at least one lane, and that lane must be one it quoted, so the used transporters can be
 * matched to distinct lanes they quoted. The bound is the size of the largest such matching over <em>all</em> quotes
 * (never more than the number of lanes or transporters). It lets the search stop as soon as it has an answer that
 * reaches the bound, instead of trying every combination of equally good transporters.
 */
final class UsageBound {

    private UsageBound() {
    }

    static int maxDistinctTransporters(AssignmentProblem problem) {
        int transporters = problem.transporterCount();
        int lanes = problem.laneCount();
        int[][] quoted = new int[transporters][];
        for (int t = 0; t < transporters; t++) {
            int[] buffer = new int[lanes];
            int size = 0;
            for (int lane = 0; lane < lanes; lane++) {
                if (problem.hasQuote(t, lane)) {
                    buffer[size++] = lane;
                }
            }
            quoted[t] = Arrays.copyOf(buffer, size);
        }

        int ceiling = Math.min(transporters, lanes);
        int[] matchedTransporter = new int[lanes];
        Arrays.fill(matchedTransporter, -1);
        int[] visitedStamp = new int[lanes];
        int matched = 0;
        for (int t = 0; t < transporters && matched < ceiling; t++) {
            if (augment(t, quoted, matchedTransporter, visitedStamp, t + 1)) {
                matched++;
            }
        }
        return matched;
    }

    private static boolean augment(int transporter, int[][] quoted, int[] matchedTransporter, int[] visitedStamp,
                                   int stamp) {
        for (int lane : quoted[transporter]) {
            if (matchedTransporter[lane] < 0) {
                matchedTransporter[lane] = transporter;
                return true;
            }
        }
        for (int lane : quoted[transporter]) {
            if (visitedStamp[lane] == stamp) {
                continue;
            }
            visitedStamp[lane] = stamp;
            if (augment(matchedTransporter[lane], quoted, matchedTransporter, visitedStamp, stamp)) {
                matchedTransporter[lane] = transporter;
                return true;
            }
        }
        return false;
    }
}
