package com.Loqal.orderservice.config; // Assuming this config is in a shared module or the OrderService

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // You can define a separate WebClient bean for each service for clarity
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // A dedicated WebClient bean for Product Service
    @Bean
    public WebClient productServiceWebClient(WebClient.Builder webClientBuilder,
                                             CircuitBreakerRegistry circuitBreakerRegistry,
                                             @Value("${services.product-service.url}") String productServiceUrl) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");

        return webClientBuilder
                .baseUrl(productServiceUrl)
                .filter((request, next) -> next.exchange(request)
                        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)))
                .build();
    }

    // A dedicated WebClient bean for User Service
    @Bean
    public WebClient userServiceWebClient(WebClient.Builder webClientBuilder,
                                          CircuitBreakerRegistry circuitBreakerRegistry,
                                          @Value("${services.user-service.url}") String userServiceUrl) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");

        return webClientBuilder
                .baseUrl(userServiceUrl)
                .filter((request, next) -> next.exchange(request)
                        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)))
                .build();
    }
}