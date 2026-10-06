package com.maybank.assessment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI assessmentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Maybank Java Backend Assessment API")
                .description("Customer account APIs with MSSQL persistence, pagination and a nested 3rd-party exchange rate call.")
                .version("v1"));
    }
}
