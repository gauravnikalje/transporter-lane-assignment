package com.assignment.transporter.optimizer;

import java.util.Arrays;

import static com.assignment.transporter.optimizer.AssignmentProblem.NO_QUOTE;

/**
 * Fast heuristic used to seed the exact search (and as its answer if the search budget runs out).
 *
 * <ol>
 *   <li>Start with the transporters that are the only bidder on some lane.</li>
 *   <li>Greedy: keep adding the transporter that improves the objective most, while under the limit.</li>
 *   <li>Local search: swap a chosen transporter for an unchosen one whenever that improves the objective.</li>
 * </ol>
 *
 * The objective is "fewest uncovered lanes, then lowest cost", so coverage always comes before price. The result
 * is valid but not guaranteed optimal, so this solver never reports {@link SolverResult.Status#OPTIMAL}.
 *
 * <p>Work is counted in lane evaluations and the cost-improving steps stop when the budget is spent, so a huge
 * input cannot keep the seed running for long. Getting every lane covered is never cut short. If the greedy cannot
 * cover every lane within the limit the result is {@link SolverResult.Status#UNKNOWN} and the exact search carries
 * on without a seed.
 */
public final class GreedyLocalSearchSolver implements AssignmentSolver {

    public static final long DEFAULT_WORK_BUDGET = 20_000_000;

    private static final int MAX_IMPROVEMENT_ROUNDS = 100;

    private final long workBudget;

    public GreedyLocalSearchSolver() {
        this(DEFAULT_WORK_BUDGET);
    }

    /** @param workBudget maximum lane evaluations */
    public GreedyLocalSearchSolver(long workBudget) {
        if (workBudget < 1) {
            throw new IllegalArgumentException("workBudget must be at least 1");
        }
        this.workBudget = workBudget;
    }

    @Override
    public SolverResult solve(AssignmentProblem problem, int maxTransporters) {
        AssignmentSolver.requireValidLimit(maxTransporters);
        RequiredTransporters required = RequiredTransporters.of(problem);
        if (required.provesInfeasible(maxTransporters)) {
            return SolverResult.infeasible(0);
        }

        Work work = new Work(workBudget);
        int transporters = problem.transporterCount();
        int limit = Math.min(maxTransporters, transporters);
        boolean[] chosen = new boolean[transporters];
        for (int t = 0; t < transporters; t++) {
            chosen[t] = required.isRequired(t);
        }
        int chosenCount = required.count();

        long[] cheapest = cheapestPerLane(problem, chosen, work);
        Objective current = Objective.of(cheapest);

        // Greedy construction. Covering every lane always finishes (it is bounded by limit x table size, which the
        // API's size caps keep small); the budget only cuts the cost-improving steps after that.
        while (chosenCount < limit && (!work.exhausted() || current.uncoveredLanes() > 0)) {
            int pick = bestAddition(problem, chosen, cheapest, current, work);
            if (pick < 0) {
                break;
            }
            chosen[pick] = true;
            chosenCount++;
            cheapest = cheapestPerLane(problem, chosen, work);
            current = Objective.of(cheapest);
        }

        // Swap-based local search.
        for (int round = 0; round < MAX_IMPROVEMENT_ROUNDS && !work.exhausted(); round++) {
            boolean improved = false;
            for (int out = 0; out < transporters && !work.exhausted(); out++) {
                if (!chosen[out] || required.isRequired(out)) {
                    continue;
                }
                chosen[out] = false;
                long[] without = cheapestPerLane(problem, chosen, work);
                int bestIn = -1;
                Objective bestObjective = current;
                for (int in = 0; in < transporters; in++) {
                    if (chosen[in] || in == out) {
                        continue;
                    }
                    work.spend(problem.laneCount());
                    Objective candidate = Objective.withTransporter(problem, without, in);
                    if (candidate.isBetterThan(bestObjective)) {
                        bestObjective = candidate;
                        bestIn = in;
                    }
                }
                if (bestIn >= 0) {
                    chosen[bestIn] = true;
                    current = bestObjective;
                    improved = true;
                } else {
                    chosen[out] = true;
                }
            }
            if (chosenCount < limit && !work.exhausted()) {
                cheapest = cheapestPerLane(problem, chosen, work);
                int pick = bestAddition(problem, chosen, cheapest, current, work);
                if (pick >= 0) {
                    chosen[pick] = true;
                    chosenCount++;
                    current = Objective.of(cheapestPerLane(problem, chosen, work));
                    improved = true;
                }
            }
            if (!improved) {
                break;
            }
        }

        return AssignmentSolution.fromChosen(problem, indicesOf(chosen))
                .map(solution -> SolverResult.feasible(solution, 0))
                .orElseGet(() -> SolverResult.unknown(0));
    }

    private static int bestAddition(AssignmentProblem problem, boolean[] chosen, long[] cheapest, Objective current,
                                    Work work) {
        int pick = -1;
        Objective best = current;
        for (int t = 0; t < problem.transporterCount(); t++) {
            if (chosen[t]) {
                continue;
            }
            work.spend(problem.laneCount());
            Objective candidate = Objective.withTransporter(problem, cheapest, t);
            if (candidate.isBetterThan(best)) {
                best = candidate;
                pick = t;
            }
        }
        return pick;
    }

    private static long[] cheapestPerLane(AssignmentProblem problem, boolean[] chosen, Work work) {
        long[] cheapest = new long[problem.laneCount()];
        Arrays.fill(cheapest, NO_QUOTE);
        for (int t = 0; t < problem.transporterCount(); t++) {
            if (!chosen[t]) {
                continue;
            }
            work.spend(cheapest.length);
            for (int lane = 0; lane < cheapest.length; lane++) {
                cheapest[lane] = Math.min(cheapest[lane], problem.cost(t, lane));
            }
        }
        return cheapest;
    }

    private static int[] indicesOf(boolean[] chosen) {
        int[] indices = new int[chosen.length];
        int size = 0;
        for (int t = 0; t < chosen.length; t++) {
            if (chosen[t]) {
                indices[size++] = t;
            }
        }
        return Arrays.copyOf(indices, size);
    }

    /** Counts lane evaluations for one {@code solve} call (kept per call so the solver itself is thread-safe). */
    private static final class Work {

        private long left;

        Work(long budget) {
            this.left = budget;
        }

        void spend(long laneEvaluations) {
            left -= laneEvaluations;
        }

        boolean exhausted() {
            return left <= 0;
        }
    }

    /** Fewest uncovered lanes first, then lowest total cost of the covered lanes. */
    private record Objective(int uncoveredLanes, long cost) {

        static Objective of(long[] cheapestPerLane) {
            int uncovered = 0;
            long cost = 0;
            for (long value : cheapestPerLane) {
                if (value == NO_QUOTE) {
                    uncovered++;
                } else {
                    cost += value;
                }
            }
            return new Objective(uncovered, cost);
        }

        static Objective withTransporter(AssignmentProblem problem, long[] cheapestPerLane, int transporter) {
            int uncovered = 0;
            long cost = 0;
            for (int lane = 0; lane < cheapestPerLane.length; lane++) {
                long value = Math.min(cheapestPerLane[lane], problem.cost(transporter, lane));
                if (value == NO_QUOTE) {
                    uncovered++;
                } else {
                    cost += value;
                }
            }
            return new Objective(uncovered, cost);
        }

        boolean isBetterThan(Objective other) {
            return uncoveredLanes != other.uncoveredLanes ? uncoveredLanes < other.uncoveredLanes : cost < other.cost;
        }
    }
}
