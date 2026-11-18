package com.assignment.transporter.api;

import org.junit.jupiter.api.BeforeEach;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.assignment.transporter.repository.LaneRepository;
import com.assignment.transporter.repository.QuoteRepository;
import com.assignment.transporter.repository.TransporterRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The two APIs end to end: real controller, validation, JPA and solver, in-memory H2. */
@SpringBootTest
@AutoConfigureMockMvc
class TransporterApiTest {

    private static final String INPUT = "/api/v1/transporters/input";
    private static final String ASSIGNMENT = "/api/v1/transporters/assignment";

    @Autowired
    MockMvc mvc;
    @Autowired
    QuoteRepository quotes;
    @Autowired
    TransporterRepository transporters;
    @Autowired
    LaneRepository lanes;

    @BeforeEach
    void emptyDatabase() {
        quotes.deleteAllInBatch();
        transporters.deleteAllInBatch();
        lanes.deleteAllInBatch();
    }

    private ResultActions submit(String json) throws Exception {
        return mvc.perform(post(INPUT).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions assign(String json) throws Exception {
        return mvc.perform(post(ASSIGNMENT).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions assign(int maxTransporters) throws Exception {
        return assign("{\"maxTransporters\": " + maxTransporters + "}");
    }

    // ---------------------------------------------------------------- API 1: input

    @Test
    void savesInputWithTheMessageFromTheBrief() throws Exception {
        submit(ApiExamples.TEST_CASE_1)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status": "success", "message": "Input data saved successfully."}""", true));
    }

    @Test
    void acceptsATransporterWithNoQuotes() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 10}]},
                                  {"id": 2, "name": "Idle", "laneQuotes": []}]}""")
                .andExpect(status().isOk());

        assign(2).andExpect(status().isOk()).andExpect(jsonPath("$.selectedTransporters[0]").value(1));
    }

    @Test
    void aNewInputReplacesTheOldOne() throws Exception {
        submit(ApiExamples.TEST_CASE_2).andExpect(status().isOk());
        submit(ApiExamples.TEST_CASE_1).andExpect(status().isOk());

        assign(3).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(60139))
                .andExpect(jsonPath("$.assignments", hasSize(5)));
    }

    @Test
    void aRejectedInputLeavesThePreviousInputUntouched() throws Exception {
        submit(ApiExamples.TEST_CASE_1).andExpect(status().isOk());
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 1, "origin": "C", "destination": "D"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": []}]}""")
                .andExpect(status().isBadRequest());

        assign(3).andExpect(status().isOk()).andExpect(jsonPath("$.totalCost").value(60139));
    }

    // ---------------------------------------------------------------- API 2: assignment, the brief's cases

    @ParameterizedTest(name = "Test Case 1, max {0} -> {1}")
    @CsvSource({"1, 96940", "2, 62938", "3, 60139", "4, 59896", "7, 59896", "100, 59896"})
    void solvesBriefTestCase1(int max, long expectedCost) throws Exception {
        submit(ApiExamples.TEST_CASE_1).andExpect(status().isOk());

        assign(max).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalCost").value(expectedCost))
                .andExpect(jsonPath("$.optimal").value(true));
    }

    @Test
    void returnsTheExactResponseShapeForTestCase1WithThreeTransporters() throws Exception {
        submit(ApiExamples.TEST_CASE_1).andExpect(status().isOk());

        assign(ApiExamples.LIMIT_3).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status": "success",
                         "totalCost": 60139,
                         "assignments": [
                           {"laneId": 1, "transporterId": 5},
                           {"laneId": 2, "transporterId": 1},
                           {"laneId": 3, "transporterId": 4},
                           {"laneId": 4, "transporterId": 4},
                           {"laneId": 5, "transporterId": 4}],
                         "selectedTransporters": [1, 4, 5],
                         "optimal": true}""", true));
    }

    @Test
    void brief_testCase2_needsThreeTransporters() throws Exception {
        submit(ApiExamples.TEST_CASE_2).andExpect(status().isOk());

        assign(3).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(7500))
                .andExpect(jsonPath("$.selectedTransporters[0]").value(1))
                .andExpect(jsonPath("$.selectedTransporters", hasSize(3)));
        assign(4).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(7450))
                .andExpect(jsonPath("$.selectedTransporters", hasSize(4)));
    }

    @Test
    void brief_testCase2_withTwoTransportersSaysHowManyAreNeeded() throws Exception {
        submit(ApiExamples.TEST_CASE_2).andExpect(status().isOk());

        assign(2).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("NO_FEASIBLE_ASSIGNMENT"))
                .andExpect(jsonPath("$.message", containsString("at least 3 transporters")))
                .andExpect(jsonPath("$.errors[0]").value("Increase maxTransporters to 3 or more."));
    }

    @Test
    void briefFirstExampleNeedsFiveTransportersNotThree() throws Exception {
        // The brief prints this example with limit 3 and cost 29000, but it cannot be done with 3 transporters.
        submit(ApiExamples.BRIEF_MAIN_EXAMPLE).andExpect(status().isOk());

        assign(3).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("at least 5 transporters")));
        assign(5).andExpect(status().isOk()).andExpect(jsonPath("$.totalCost").value(48000));
    }

    @ParameterizedTest(name = "data file, max {0} -> {1}")
    @CsvSource({"3, 134876", "4, 127844", "5, 125045"})
    void solvesTheProvidedDataFile(int max, long expectedCost) throws Exception {
        submit(ApiExamples.DATA_FILE_9_LANES).andExpect(status().isOk());

        assign(max).andExpect(status().isOk()).andExpect(jsonPath("$.totalCost").value(expectedCost));
    }

    @Test
    void listsAssignmentsInLaneOrderAndTransportersInIdOrder() throws Exception {
        submit(ApiExamples.DATA_FILE_9_LANES).andExpect(status().isOk());

        assign(4).andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].laneId").value(1))
                .andExpect(jsonPath("$.assignments[8].laneId").value(9))
                .andExpect(jsonPath("$.selectedTransporters[0]").value(1))
                .andExpect(jsonPath("$.selectedTransporters[3]").value(7));
    }

    @Test
    void anEqualQuoteGoesToTheLowerTransporterId() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 9, "name": "Nine", "laneQuotes": [{"laneId": 1, "quote": 500}]},
                                  {"id": 3, "name": "Three", "laneQuotes": [{"laneId": 1, "quote": 500}]}]}""")
                .andExpect(status().isOk());

        assign(2).andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].transporterId").value(3))
                .andExpect(jsonPath("$.selectedTransporters", hasSize(1)));
    }

    // ---------------------------------------------------------------- API 2: errors

    @Test
    void askingForAnAssignmentBeforeAnyInputIsAConflict() throws Exception {
        assign(3).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_INPUT_DATA"))
                .andExpect(jsonPath("$.message", containsString("POST /api/v1/transporters/input")));
    }

    @Test
    void aLaneNobodyQuotedIsReportedByName() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 2, "origin": "C", "destination": "D"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 100}]}]}""")
                .andExpect(status().isOk());

        assign(5).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_FEASIBLE_ASSIGNMENT"))
                .andExpect(jsonPath("$.errors[0]").value("Lane 2 has no quote from any transporter"));
    }

    @ParameterizedTest(name = "bad assignment body: {0}")
    @CsvSource(delimiter = '|', value = {
            "{}                              | maxTransporters is required",
            "{\"maxTransporters\": 0}        | maxTransporters must be at least 1",
            "{\"maxTransporters\": -2}       | maxTransporters must be at least 1",
            "{\"maxTransporters\": null}     | maxTransporters is required"})
    void rejectsAnInvalidLimit(String body, String expectedError) throws Exception {
        submit(ApiExamples.TEST_CASE_1).andExpect(status().isOk());

        assign(body.trim()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0]").value(expectedError));
    }

    @ParameterizedTest(name = "unreadable assignment body: {0}")
    @CsvSource(delimiter = '|', value = {
            "{\"maxTransporters\": \"three\"}",
            "{\"maxTransporters\": 2.5}",
            "{\"maxTransporters\": ",
            "not json"})
    void rejectsAnUnreadableLimit(String body) throws Exception {
        assign(body.trim()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsAnEmptyBody() throws Exception {
        mvc.perform(post(ASSIGNMENT).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // ---------------------------------------------------------------- API 1: validation

    @Test
    void rejectsEmptyLanesAndTransporters() throws Exception {
        submit("{\"lanes\": [], \"transporters\": []}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasItem("lanes must contain at least one lane")))
                .andExpect(jsonPath("$.errors", hasItem("transporters must contain at least one transporter")));
    }

    @Test
    void rejectsMissingFieldsWithTheFieldPath() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": " ", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 0}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("lanes[0].origin must not be blank")))
                .andExpect(jsonPath("$.errors", hasItem("transporters[0].laneQuotes[0].quote must be greater than 0")));
    }

    @Test
    void rejectsANullQuoteList() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("transporters[0].laneQuotes")));
    }

    @Test
    void rejectsANullEntryInTheLists() throws Exception {
        submit("""
                {"lanes": [null], "transporters": [{"id": 1, "name": "T1", "laneQuotes": []}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("lanes[0] must not be null"));
    }

    @Test
    void rejectsADecimalQuoteInsteadOfRoundingItDown() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 5000.75}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("transporters[0].laneQuotes[0].quote")));
    }

    @Test
    void rejectsATextQuote() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": "cheap"}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsDuplicateLaneIds() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 1, "origin": "C", "destination": "D"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 5}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0]").value("lanes[1]: lane id 1 is used more than once"));
    }

    @Test
    void rejectsDuplicateTransporterIds() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 5}]},
                                  {"id": 1, "name": "Again", "laneQuotes": []}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("transporters[1]: transporter id 1 is used more than once"));
    }

    @Test
    void rejectsAQuoteForALaneThatDoesNotExist() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 99, "quote": 5}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]")
                        .value("transporters[0].laneQuotes[0]: lane 99 is not in the lanes list"));
    }

    @Test
    void rejectsTheSameLaneQuotedTwiceByOneTransporter() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1",
                                   "laneQuotes": [{"laneId": 1, "quote": 5}, {"laneId": 1, "quote": 6}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("quoted lane 1 more than once")));
    }

    @Test
    void rejectsAHugeQuoteInsteadOfOverflowingLater() throws Exception {
        // Found in review: this used to be accepted and then every assignment call returned a 500.
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 2, "origin": "C", "destination": "D"}],
                 "transporters": [
                   {"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 1}, {"laneId": 2, "quote": 1}]},
                   {"id": 2, "name": "T2", "laneQuotes": [{"laneId": 1, "quote": 5000000000000000000},
                                                           {"laneId": 2, "quote": 9223372036854775807}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0]", containsString("must be less than or equal to 1000000000000")));
    }

    @Test
    void acceptsTheLargestQuoteAndAddsThemWithoutOverflow() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 2, "origin": "C", "destination": "D"}],
                 "transporters": [
                   {"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 1000000000000},
                                                           {"laneId": 2, "quote": 1000000000000}]}]}""")
                .andExpect(status().isOk());

        assign(1).andExpect(status().isOk()).andExpect(jsonPath("$.totalCost").value(2_000_000_000_000L));
    }

    @Test
    void rejectsNumbersSentAsText() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": "5000"}]}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        assign("{\"maxTransporters\": \"3\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void tiedQuotesAreSharedOutSoMoreTransportersAreUsed() throws Exception {
        // Both transporters quote 100 on both lanes. Using both is as cheap as using one, and the brief wants the
        // most transporters up to the limit.
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 2, "origin": "C", "destination": "D"}],
                 "transporters": [
                   {"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 100}, {"laneId": 2, "quote": 100}]},
                   {"id": 2, "name": "T2", "laneQuotes": [{"laneId": 1, "quote": 100}, {"laneId": 2, "quote": 100}]}]}""")
                .andExpect(status().isOk());

        assign(2).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(200))
                .andExpect(jsonPath("$.selectedTransporters[0]").value(1))
                .andExpect(jsonPath("$.selectedTransporters[1]").value(2))
                .andExpect(jsonPath("$.assignments[0].transporterId").value(1))
                .andExpect(jsonPath("$.assignments[1].transporterId").value(2));
        assign(1).andExpect(status().isOk()).andExpect(jsonPath("$.selectedTransporters", hasSize(1)));
    }

    @Test
    void acceptsNamesWithSurroundingSpaces() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "  Mumbai ", "destination": "Delhi"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 10}]}]}""")
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- hostile bodies (found in review)

    private static final String ONE_TRANSPORTER = "\"transporters\":[{\"id\":1,\"name\":\"T\",\"laneQuotes\":[]}]";

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void aBodyOfThousandsOfEmptyObjectsIsRejectedQuicklyWithASmallResponse() throws Exception {
        // 30,000 lanes of {}: used to build about a million violation objects and could exhaust the heap.
        String body = "{\"lanes\":[" + "{},".repeat(29_999) + "{}]," + ONE_TRANSPORTER + "}";

        String response = submit(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response.length()).isLessThan(1_000);
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void aBodyOfHundredsOfThousandsOfNullEntriesIsRejectedQuickly() throws Exception {
        // 100,000 nulls: the validator's cost grew faster than the number of entries and took minutes.
        String body = "{\"lanes\":[" + "null,".repeat(99_999) + "null]," + ONE_TRANSPORTER + "}";

        submit(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void manyBadFieldsStillGiveAShortErrorList() throws Exception {
        // 1,500 lanes (inside the cap) that are all {}: every field is missing, but only 20 messages come back.
        String body = "{\"lanes\":[" + "{},".repeat(1_499) + "{}]," + ONE_TRANSPORTER + "}";

        submit(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(21)))
                .andExpect(jsonPath("$.errors[20]").value(containsString("more")));
    }

    // ---------------------------------------------------------------- framework errors use the same shape

    @Test
    void wrongMethodGetsAJsonError() throws Exception {
        mvc.perform(get(INPUT)).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void wrongContentTypeGetsAJsonError() throws Exception {
        mvc.perform(post(INPUT).contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unknownUrlGetsAJsonError() throws Exception {
        mvc.perform(get("/api/v1/nothing-here")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))));
    }
}
