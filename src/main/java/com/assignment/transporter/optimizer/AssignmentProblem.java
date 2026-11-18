package com.assignment.transporter.optimizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable, index-based view of one assignment problem: the lanes, the transporters and what each
 * transporter quoted for each lane.
 *
 * <p>Lanes and transporters are stored in ascending id order, so index order equals id order. Every solver
 * relies on that to break ties the same way (an equal quote goes to the lower transporter id).
 *
 * <p>Costs are plain {@code long} values in whatever unit the caller chooses (the service uses whole rupees).
 */
public final class AssignmentProblem {

    /** Marks "this transporter did not quote this lane". */
    public static final long NO_QUOTE = Long.MAX_VALUE;

    private final long[] laneIds;
    private final long[] transporterIds;
    private final long[][] costs;

    private AssignmentProblem(long[] laneIds, long[] transporterIds, long[][] costs) {
        this.laneIds = laneIds;
        this.transporterIds = transporterIds;
        this.costs = costs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int laneCount() {
        return laneIds.length;
    }

    public int transporterCount() {
        return transporterIds.length;
    }

    public long laneId(int laneIndex) {
        return laneIds[laneIndex];
    }

    public long transporterId(int transporterIndex) {
        return transporterIds[transporterIndex];
    }

    /** The quote of a transporter for a lane, or {@link #NO_QUOTE}. */
    public long cost(int transporterIndex, int laneIndex) {
        return costs[transporterIndex][laneIndex];
    }

    public boolean hasQuote(int transporterIndex, int laneIndex) {
        return costs[transporterIndex][laneIndex] != NO_QUOTE;
    }

    /** Collects lanes, transporters and quotes in any order and freezes them into a problem. */
    public static final class Builder {

        private final SortedSet<Long> lanes = new TreeSet<>();
        private final SortedMap<Long, Map<Long, Long>> quotesByTransporter = new TreeMap<>();

        private Builder() {
        }

        public Builder lane(long laneId) {
            lanes.add(laneId);
            return this;
        }

        /** Registers a transporter, even one without quotes. */
        public Builder transporter(long transporterId) {
            quotesByTransporter.computeIfAbsent(transporterId, id -> new HashMap<>());
            return this;
        }

        public Builder quote(long transporterId, long laneId, long cost) {
            if (cost < 0 || cost == NO_QUOTE) {
                throw new IllegalArgumentException("Cost must be zero or more and below Long.MAX_VALUE, got " + cost);
            }
            Map<Long, Long> byLane = quotesByTransporter.computeIfAbsent(transporterId, id -> new HashMap<>());
            if (byLane.putIfAbsent(laneId, cost) != null) {
                throw new IllegalArgumentException(
                        "Transporter " + transporterId + " quoted lane " + laneId + " more than once");
            }
            return this;
        }

        public AssignmentProblem build() {
            long[] laneIds = lanes.stream().mapToLong(Long::longValue).toArray();
            Map<Long, Integer> laneIndexById = new HashMap<>();
            for (int i = 0; i < laneIds.length; i++) {
                laneIndexById.put(laneIds[i], i);
            }

            long[] transporterIds = quotesByTransporter.keySet().stream().mapToLong(Long::longValue).toArray();
            long[][] costs = new long[transporterIds.length][laneIds.length];
            int t = 0;
            for (Map.Entry<Long, Map<Long, Long>> transporter : quotesByTransporter.entrySet()) {
                Arrays.fill(costs[t], NO_QUOTE);
                for (Map.Entry<Long, Long> quote : transporter.getValue().entrySet()) {
                    Integer laneIndex = laneIndexById.get(quote.getKey());
                    if (laneIndex == null) {
                        throw new IllegalArgumentException(
                                "Transporter " + transporter.getKey() + " quoted unknown lane " + quote.getKey());
                    }
                    costs[t][laneIndex] = quote.getValue();
                }
                t++;
            }
            return new AssignmentProblem(laneIds, transporterIds, costs);
        }
    }
}
