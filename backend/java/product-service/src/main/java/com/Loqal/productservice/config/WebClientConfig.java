package com.Loqal.productservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }

    @Bean
    public WebClient productServiceWebClient(WebClient.Builder webClientBuilder,
                                             CircuitBreakerRegistry circuitBreakerRegistry,
                                             @Value("${services.product-service.url}") String productServiceUrl) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");

        return webClientBuilder
                .baseUrl(productServiceUrl)
                .filter((request, next) -> next.exchange(request)
                        .timeout(Duration.ofSeconds(8)) // Response timeout
                        .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> !(throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException)))
                        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                        .onErrorResume(throwable -> {
                            if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                                return Mono.error(new RuntimeException("Product service is currently unavailable"));
                            }
                            return Mono.error(throwable);
                        }))
                .build();
    }

    @Bean
    public WebClient userServiceWebClient(WebClient.Builder webClientBuilder,
                                          CircuitBreakerRegistry circuitBreakerRegistry,
                                          @Value("${services.user-service.url}") String userServiceUrl) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");

        return webClientBuilder
                .baseUrl(userServiceUrl)
                .filter((request, next) -> next.exchange(request)
                        .timeout(Duration.ofSeconds(8))
                        .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> !(throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException)))
                        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                        .onErrorResume(throwable -> {
                            if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                                return Mono.error(new RuntimeException("User service is currently unavailable"));
                            }
                            return Mono.error(throwable);
                        }))
                .build();
    }
}