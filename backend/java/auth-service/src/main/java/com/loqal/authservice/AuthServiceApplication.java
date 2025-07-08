package com.loqal.authservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@OpenAPIDefinition(
        info = @Info(
                title = "Authentication Service API",
                version = "v1",
                description = "Service to manage authentication for all types of users",
                contact = @Contact(name = "Anubhav", email = "anubhavauth@gmail.com")
        )
)
@EntityScan("com.loqal.authservice")
@EnableJpaRepositories("com.loqal.authservice")
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
