package com.assignment.transporter.optimizer;

/**
 * Facts every solver checks first: whether each lane has at least one quote, and which transporters are the only
 * bidder on some lane (they must be in any valid answer).
 */
final class RequiredTransporters {

    private final boolean[] required;
    private final int count;
    private final boolean everyLaneQuoted;

    private RequiredTransporters(boolean[] required, int count, boolean everyLaneQuoted) {
        this.required = required;
        this.count = count;
        this.everyLaneQuoted = everyLaneQuoted;
    }

    static RequiredTransporters of(AssignmentProblem problem) {
        boolean[] required = new boolean[problem.transporterCount()];
        int count = 0;
        boolean everyLaneQuoted = true;
        for (int lane = 0; lane < problem.laneCount(); lane++) {
            int bidders = 0;
            int onlyBidder = -1;
            for (int transporter = 0; transporter < problem.transporterCount() && bidders < 2; transporter++) {
                if (problem.hasQuote(transporter, lane)) {
                    bidders++;
                    onlyBidder = transporter;
                }
            }
            if (bidders == 0) {
                everyLaneQuoted = false;
            } else if (bidders == 1 && !required[onlyBidder]) {
                required[onlyBidder] = true;
                count++;
            }
        }
        return new RequiredTransporters(required, count, everyLaneQuoted);
    }

    /** True when no set of at most {@code maxTransporters} transporters can possibly cover every lane. */
    boolean provesInfeasible(int maxTransporters) {
        return !everyLaneQuoted || count > maxTransporters;
    }

    boolean isRequired(int transporterIndex) {
        return required[transporterIndex];
    }

    int count() {
        return count;
    }
}
