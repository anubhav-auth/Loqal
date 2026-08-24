package com.loqal.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(scanBasePackages = "com.loqal")
@EnableR2dbcRepositories(basePackages = "com.loqal")
public class LoqalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoqalApplication.class, args);
    }
}
