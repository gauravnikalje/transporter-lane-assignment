package com.assignment.transporter.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentationAndHealthTest {

    @Autowired
    MockMvc mvc;

    @Test
    void theSiteRootOpensSwagger() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/swagger-ui.html"));
    }

    @Test
    void theOpenApiDocumentDescribesBothApisAndCarriesTheBriefExamples() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Transporter Assignment on Lanes API"))
                .andExpect(jsonPath("$.paths['/api/v1/transporters/input'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transporters/assignment'].post").exists())
                .andExpect(content().string(containsString("Brief Test Case 1")))
                .andExpect(content().string(containsString("Brief Test Case 2")))
                .andExpect(content().string(containsString("Provided data file")));
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void healthEndpointReportsUpForTheHost() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
