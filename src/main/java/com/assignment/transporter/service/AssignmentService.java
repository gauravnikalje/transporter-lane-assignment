package com.assignment.transporter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.springframework.stereotype.Service;

import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;
import com.assignment.transporter.optimizer.AssignmentProblem;
import com.assignment.transporter.optimizer.AssignmentSolver;
import com.assignment.transporter.optimizer.CoverageAnalyzer;
import com.assignment.transporter.optimizer.SolverResult;
import com.assignment.transporter.service.exception.InfeasibleAssignmentException;
import com.assignment.transporter.service.exception.NoInputDataException;
import com.assignment.transporter.service.exception.SearchLimitReachedException;

/** Loads the stored input, runs the solver and turns "no answer" outcomes into clear errors. */
@Service
public class AssignmentService {

    private final InputDataStore store;
    private final AssignmentSolver solver;
    private final CoverageAnalyzer coverageAnalyzer;

    public AssignmentService(InputDataStore store, AssignmentSolver solver, CoverageAnalyzer coverageAnalyzer) {
        this.store = store;
        this.solver = solver;
        this.coverageAnalyzer = coverageAnalyzer;
    }

    public AssignmentResult assign(int maxTransporters) {
        StoredInput input = store.load().orElseThrow(NoInputDataException::new);
        AssignmentProblem problem = toProblem(input);

        SolverResult result = solver.solve(problem, maxTransporters);
        return switch (result.status()) {
            case OPTIMAL -> new AssignmentResult(result.solution(), true);
            case FEASIBLE -> new AssignmentResult(result.solution(), false);
            case INFEASIBLE -> throw infeasible(problem, maxTransporters);
            case UNKNOWN -> throw new SearchLimitReachedException(maxTransporters);
        };
    }

    private InfeasibleAssignmentException infeasible(AssignmentProblem problem, int maxTransporters) {
        List<String> unquotedLanes = new ArrayList<>();
        for (int lane = 0; lane < problem.laneCount(); lane++) {
            boolean quoted = false;
            for (int t = 0; t < problem.transporterCount() && !quoted; t++) {
                quoted = problem.hasQuote(t, lane);
            }
            if (!quoted) {
                unquotedLanes.add("Lane " + problem.laneId(lane) + " has no quote from any transporter");
            }
        }
        if (!unquotedLanes.isEmpty()) {
            return new InfeasibleAssignmentException(
                    "Every lane needs at least one quote, so no assignment is possible.", unquotedLanes);
        }

        OptionalInt needed = coverageAnalyzer.minimumTransportersToCover(problem);
        if (needed.isPresent()) {
            return new InfeasibleAssignmentException("Covering every lane needs at least " + needed.getAsInt()
                    + " transporters, but maxTransporters is " + maxTransporters + ".",
                    List.of("Increase maxTransporters to " + needed.getAsInt() + " or more."));
        }
        return new InfeasibleAssignmentException("No assignment covering every lane was found with at most "
                + maxTransporters + " transporters.", List.of());
    }

    private static AssignmentProblem toProblem(StoredInput input) {
        AssignmentProblem.Builder builder = AssignmentProblem.builder();
        for (LaneEntity lane : input.lanes()) {
            builder.lane(lane.getId());
        }
        for (TransporterEntity transporter : input.transporters()) {
            builder.transporter(transporter.getId());
        }
        for (QuoteEntity quote : input.quotes()) {
            builder.quote(quote.getId().transporterId(), quote.getId().laneId(), quote.getAmount());
        }
        return builder.build();
    }
}
