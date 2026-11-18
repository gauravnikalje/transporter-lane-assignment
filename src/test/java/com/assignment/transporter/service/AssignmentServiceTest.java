package com.assignment.transporter.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.assignment.transporter.domain.LaneEntity;
import com.assignment.transporter.domain.QuoteEntity;
import com.assignment.transporter.domain.TransporterEntity;
import com.assignment.transporter.optimizer.AssignmentSolution;
import com.assignment.transporter.optimizer.AssignmentSolver;
import com.assignment.transporter.optimizer.CoverageAnalyzer;
import com.assignment.transporter.optimizer.SolverResult;
import com.assignment.transporter.service.exception.InfeasibleAssignmentException;
import com.assignment.transporter.service.exception.NoInputDataException;
import com.assignment.transporter.service.exception.SearchLimitReachedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    InputDataStore store;

    private final CoverageAnalyzer coverage = new CoverageAnalyzer(100_000);

    /** Lane 1 and 2, transporter 1 quotes lane 1 only, transporter 2 quotes lane 2 only. */
    private StoredInput twoLanesTwoSpecialists() {
        return new StoredInput(
                List.of(new LaneEntity(1L, "A", "B"), new LaneEntity(2L, "C", "D")),
                List.of(new TransporterEntity(1L, "T1"), new TransporterEntity(2L, "T2")),
                List.of(new QuoteEntity(1L, 1L, 10), new QuoteEntity(2L, 2L, 20)));
    }

    private AssignmentService serviceWith(AssignmentSolver solver) {
        return new AssignmentService(store, solver, coverage);
    }

    private static AssignmentSolution someSolution() {
        return new AssignmentSolution(30, Map.of(1L, 1L, 2L, 2L), List.of(1L, 2L));
    }

    @Test
    void failsWhenNothingWasSubmitted() {
        when(store.load()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceWith((problem, max) -> SolverResult.unknown(0)).assign(2))
                .isInstanceOf(NoInputDataException.class);
    }

    @Test
    void marksAnAnswerOptimalWhenTheSearchFinished() {
        when(store.load()).thenReturn(Optional.of(twoLanesTwoSpecialists()));

        AssignmentResult result = serviceWith((problem, max) -> SolverResult.optimal(someSolution(), 5)).assign(2);

        assertThat(result.optimal()).isTrue();
        assertThat(result.solution().totalCost()).isEqualTo(30);
    }

    @Test
    void marksAnAnswerNotOptimalWhenTheSearchBudgetRanOut() {
        when(store.load()).thenReturn(Optional.of(twoLanesTwoSpecialists()));

        AssignmentResult result = serviceWith((problem, max) -> SolverResult.feasible(someSolution(), 5)).assign(2);

        assertThat(result.optimal()).isFalse();
    }

    @Test
    void failsWithTheSearchLimitWhenNoAnswerWasFoundInTime() {
        when(store.load()).thenReturn(Optional.of(twoLanesTwoSpecialists()));

        assertThatThrownBy(() -> serviceWith((problem, max) -> SolverResult.unknown(5)).assign(2))
                .isInstanceOf(SearchLimitReachedException.class)
                .hasMessageContaining("at most 2 transporters");
    }

    @Test
    void explainsAnInfeasibleLimitWithTheNumberNeeded() {
        when(store.load()).thenReturn(Optional.of(twoLanesTwoSpecialists()));

        assertThatThrownBy(() -> serviceWith((problem, max) -> SolverResult.infeasible(0)).assign(1))
                .isInstanceOfSatisfying(InfeasibleAssignmentException.class, ex -> {
                    assertThat(ex).hasMessageContaining("at least 2 transporters").hasMessageContaining("is 1");
                    assertThat(ex.details()).containsExactly("Increase maxTransporters to 2 or more.");
                });
    }

    @Test
    void fallsBackToAGenericMessageWhenTheCoverageSearchRunsOutOfBudget() {
        when(store.load()).thenReturn(Optional.of(twoLanesTwoSpecialists()));
        AssignmentService service = new AssignmentService(store, (problem, max) -> SolverResult.infeasible(0),
                new CoverageAnalyzer(1));

        assertThatThrownBy(() -> service.assign(1))
                .isInstanceOfSatisfying(InfeasibleAssignmentException.class, ex -> {
                    assertThat(ex).hasMessageContaining("No assignment covering every lane was found");
                    assertThat(ex.details()).isEmpty();
                });
    }
}
