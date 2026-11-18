package com.assignment.transporter.optimizer;

import java.util.Objects;
import java.util.Optional;

/**
 * What a solver found.
 *
 * <ul>
 *   <li>{@link Status#OPTIMAL}: the search finished; no better assignment exists.</li>
 *   <li>{@link Status#FEASIBLE}: a valid assignment, but the search stopped early (node budget), so a better
 *       one may exist.</li>
 *   <li>{@link Status#INFEASIBLE}: proven that no set of transporters within the limit covers every lane.</li>
 *   <li>{@link Status#UNKNOWN}: the search stopped early without finding a valid assignment.</li>
 * </ul>
 */
public record SolverResult(Status status, AssignmentSolution solution, long nodesExplored) {

    public enum Status { OPTIMAL, FEASIBLE, INFEASIBLE, UNKNOWN }

    public SolverResult {
        Objects.requireNonNull(status, "status");
        boolean needsSolution = status == Status.OPTIMAL || status == Status.FEASIBLE;
        if (needsSolution != (solution != null)) {
            throw new IllegalArgumentException("Status " + status + (needsSolution ? " needs" : " must not have")
                    + " a solution");
        }
    }

    public static SolverResult optimal(AssignmentSolution solution, long nodesExplored) {
        return new SolverResult(Status.OPTIMAL, solution, nodesExplored);
    }

    public static SolverResult feasible(AssignmentSolution solution, long nodesExplored) {
        return new SolverResult(Status.FEASIBLE, solution, nodesExplored);
    }

    public static SolverResult infeasible(long nodesExplored) {
        return new SolverResult(Status.INFEASIBLE, null, nodesExplored);
    }

    public static SolverResult unknown(long nodesExplored) {
        return new SolverResult(Status.UNKNOWN, null, nodesExplored);
    }

    public Optional<AssignmentSolution> solutionIfAny() {
        return Optional.ofNullable(solution);
    }
}
