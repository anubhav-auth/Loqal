package com.loqal.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {
//    @Bean
//    public CircuitBreakerConfig circuitBreakerConfig() {
//        return CircuitBreakerConfig.custom()
//                .failureRateThreshold(50)
//                .waitDurationInOpenState(Duration.ofSeconds(10))
//                .slidingWindowSize(10)
//                .build();
//    }
//
//    @Bean
//    public RetryConfig_retryConfig() {
//        return RetryConfig.custom()
//                .maxAttempts(3)
//                .waitDuration(Duration.ofMillis(500))
//                .build();
//    }
}