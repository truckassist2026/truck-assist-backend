package com.truckassist.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI truckAssistOpenAPI() {

        return new OpenAPI()

                // =================================================
                // API INFORMATION
                // =================================================

                .info(
                        new Info()
                                .title("Truck Assist API")
                                .version("1.0.0")
                                .description(
                                        "Truck Assist Driver and Mechanic API"
                                )
                                .contact(
                                        new Contact()
                                                .name("Truck Assist")
                                )
                )

                // =================================================
                // JWT SECURITY SCHEME
                // =================================================

                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(
                                                        SECURITY_SCHEME_NAME
                                                )
                                                .type(
                                                        SecurityScheme.Type.HTTP
                                                )
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                )

                // =================================================
                // GLOBAL SECURITY
                // =================================================

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(
                                        SECURITY_SCHEME_NAME
                                )
                );
    }
}