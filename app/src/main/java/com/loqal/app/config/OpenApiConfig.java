package com.loqal.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Single OpenAPI definition for the whole platform (PRD XC-103). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI loqalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loqal Platform API")
                        .version("1.0.0")
                        .description("Self-hostable commerce backend: identity, catalog, orders, payments"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
