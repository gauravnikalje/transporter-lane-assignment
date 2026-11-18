package com.assignment.transporter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Swagger metadata, and a redirect so the site root opens the documentation. */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    OpenAPI transporterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Transporter Assignment on Lanes API")
                .version("1.0.0")
                .description("""
                        Submit lanes and transporter quotes, then ask for the cheapest assignment that covers \
                        every lane using at most N transporters.

                        **Try it:** open *POST /api/v1/transporters/input*, pick an example from the drop-down \
                        (Test Case 1 is the brief's), press Execute, then call *POST /api/v1/transporters/assignment*.

                        Rules: every lane goes to the cheapest chosen transporter that quoted it; lowest total cost \
                        wins; on equal cost, more transporters used wins. Quotes are whole rupees. Data is kept in \
                        memory and each input replaces the previous one."""));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }
}
