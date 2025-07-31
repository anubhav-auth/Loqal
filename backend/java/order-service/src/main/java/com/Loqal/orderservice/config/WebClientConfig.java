package com.Loqal.orderservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${services.merchant-service.url}")
    private String merchantServiceUrl;

    @Value("${services.product-service.url}")
    private String productServiceUrl;

    @Bean
    public WebClient webClient(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker userServiceCircuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        CircuitBreaker merchantServiceCircuitBreaker = circuitBreakerRegistry.circuitBreaker("merchantService");
        CircuitBreaker productServiceCircuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");

        return WebClient.builder()
                .baseUrl(userServiceUrl) // Default base URL; override per service
                .filter((request, next) -> {
                    String host = request.url().getHost();

                    CircuitBreaker circuitBreaker;

                    if (host.contains("merchant")) {
                        circuitBreaker = merchantServiceCircuitBreaker;
                    } else if (host.contains("product")) {
                        circuitBreaker = productServiceCircuitBreaker;
                    } else {
                        circuitBreaker = userServiceCircuitBreaker; // default fallback
                    }

                    return next.exchange(request)
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
                })
                .build();
    }
}