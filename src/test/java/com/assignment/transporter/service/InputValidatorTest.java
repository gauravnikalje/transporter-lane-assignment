package com.assignment.transporter.service;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.assignment.transporter.api.dto.InputRequest;
import com.assignment.transporter.api.dto.LaneDto;
import com.assignment.transporter.api.dto.LaneQuoteDto;
import com.assignment.transporter.api.dto.TransporterDto;
import com.assignment.transporter.config.TransporterProperties;
import com.assignment.transporter.service.exception.InputValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputValidatorTest {

    private final InputValidator validator = new InputValidator(
            new TransporterProperties(new TransporterProperties.Solver(1_000, 1_000),
                    new TransporterProperties.Limits(3, 3, 4, 1_000)));

    private static LaneDto lane(long id) {
        return new LaneDto(id, "From " + id, "To " + id);
    }

    private static TransporterDto transporter(long id, LaneQuoteDto... quotes) {
        return new TransporterDto(id, "T" + id, List.of(quotes));
    }

    private static LaneQuoteDto quote(long laneId, long amount) {
        return new LaneQuoteDto(laneId, amount);
    }

    private static List<String> errorsOf(InputRequest request, InputValidator validator) {
        return assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InputValidationException.class)
                .actual() instanceof InputValidationException ex ? ex.errors() : List.of();
    }

    @Test
    void acceptsAConsistentRequest() {
        InputRequest request = new InputRequest(List.of(lane(1), lane(2)),
                List.of(transporter(1, quote(1, 10), quote(2, 20)), transporter(2, quote(2, 5))));

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void reportsEveryProblemAtOnce() {
        InputRequest request = new InputRequest(List.of(lane(1), lane(1)),
                List.of(transporter(7, quote(1, 10), quote(1, 11), quote(9, 5)), transporter(7)));

        assertThat(errorsOf(request, validator)).containsExactly(
                "lanes[1]: lane id 1 is used more than once",
                "transporters[0].laneQuotes[1]: transporter 7 quoted lane 1 more than once",
                "transporters[0].laneQuotes[2]: lane 9 is not in the lanes list",
                "transporters[1]: transporter id 7 is used more than once");
    }

    @Test
    void enforcesTheSizeCaps() {
        InputRequest tooMany = new InputRequest(
                List.of(lane(1), lane(2), lane(3), lane(4)),
                List.of(transporter(1), transporter(2), transporter(3), transporter(4)));

        assertThat(errorsOf(tooMany, validator)).containsExactly(
                "At most 3 lanes are accepted, got 4",
                "At most 3 transporters are accepted, got 4");
    }

    @Test
    void enforcesTheTableSizeCap() {
        InputValidator tight = new InputValidator(new TransporterProperties(
                new TransporterProperties.Solver(1_000, 1_000), new TransporterProperties.Limits(100, 100, 1_000, 5)));
        InputRequest request = new InputRequest(List.of(lane(1), lane(2), lane(3)),
                List.of(transporter(1, quote(1, 1)), transporter(2, quote(2, 1))));

        assertThat(errorsOf(request, tight))
                .containsExactly("Lanes x transporters must be at most 5, got 3 x 2 = 6");
    }

    @Test
    void enforcesTheTotalQuoteCap() {
        InputRequest request = new InputRequest(List.of(lane(1), lane(2), lane(3)),
                List.of(transporter(1, quote(1, 1), quote(2, 1), quote(3, 1)), transporter(2, quote(1, 1), quote(2, 1))));

        assertThat(errorsOf(request, validator)).containsExactly("At most 4 quotes are accepted in total, got 5");
    }

    @Test
    void capsTheNumberOfMessages() {
        InputValidator wide = new InputValidator(new TransporterProperties(
                new TransporterProperties.Solver(1_000, 1_000), new TransporterProperties.Limits(100, 100, 1_000, 100_000)));
        List<LaneQuoteDto> unknownLanes = IntStream.rangeClosed(1, 50).mapToObj(i -> quote(1_000 + i, 1)).toList();
        InputRequest request = new InputRequest(List.of(lane(1)),
                List.of(new TransporterDto(1L, "T1", unknownLanes)));

        List<String> errors = errorsOf(request, wide);

        assertThat(errors).hasSize(21);
        assertThat(errors.get(20)).isEqualTo("... and more");
    }
}
