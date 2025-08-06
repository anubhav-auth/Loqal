package com.loqal.authservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@OpenAPIDefinition(
        info = @Info(
                title = "Authentication Service API",
                version = "v1",
                description = "Service to manage authentication for all types of users",
                contact = @Contact(name = "Anubhav", email = "anubhavauth@gmail.com")
        )
)
@EnableR2dbcRepositories("com.loqal.authservice.repository")
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
