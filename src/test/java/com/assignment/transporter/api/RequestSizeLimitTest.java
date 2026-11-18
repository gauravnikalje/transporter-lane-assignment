package com.assignment.transporter.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.assignment.transporter.api.error.PayloadTooLargeException;
import com.assignment.transporter.config.TransporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A body above the limit is refused before it is parsed. The limit is set low here so the tests stay small. */
@SpringBootTest(properties = "transporter.limits.max-body-bytes=1000")
@AutoConfigureMockMvc
class RequestSizeLimitTest {

    @Autowired
    MockMvc mvc;

    @Test
    void refusesABodyThatDeclaresItIsTooLarge() throws Exception {
        mvc.perform(post("/api/v1/transporters/input").contentType(MediaType.APPLICATION_JSON)
                        .content(ApiExamples.TEST_CASE_1))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("The request body must be at most 1000 bytes."));
    }

    @Test
    void letsASmallBodyThrough() throws Exception {
        // Reaches the controller: with no input stored yet the answer is the 409, not a 413.
        mvc.perform(post("/api/v1/transporters/assignment").contentType(MediaType.APPLICATION_JSON)
                        .content(ApiExamples.LIMIT_3))
                .andExpect(status().isConflict());
    }

    @Test
    void cutsOffABodyThatDoesNotDeclareItsSizeOnceItPassesTheLimit() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(new TransporterProperties(
                new TransporterProperties.Solver(1, 1, 1), new TransporterProperties.Limits(1, 1, 1, 1, 10)));
        byte[] body = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest chunked = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return -1; // as with Transfer-Encoding: chunked
            }
        };
        chunked.setContent(body);
        AtomicReference<IOException> thrown = new AtomicReference<>();

        filter.doFilter(chunked, new MockHttpServletResponse(), (request, response) -> {
            try {
                request.getInputStream().readAllBytes();
            } catch (IOException e) {
                thrown.set(e);
            }
        });

        assertThat(thrown.get()).isInstanceOf(PayloadTooLargeException.class).hasMessageContaining("10 bytes");
    }

    @Test
    void letsAChunkedBodyUnderTheLimitThroughIntact() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(new TransporterProperties(
                new TransporterProperties.Solver(1, 1, 1), new TransporterProperties.Limits(1, 1, 1, 1, 10)));
        MockHttpServletRequest chunked = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        chunked.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(chunked, new MockHttpServletResponse(), (request, response) -> {
            try (var reader = request.getReader()) {
                seen.set(reader.readLine());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(seen.get()).isEqualTo("0123456789");
    }
}
