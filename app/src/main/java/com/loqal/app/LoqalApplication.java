package com.loqal.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.loqal")
public class LoqalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoqalApplication.class, args);
    }
}
