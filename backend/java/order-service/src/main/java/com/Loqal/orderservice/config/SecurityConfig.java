package com.Loqal.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.jwks-uri}")
    private String jwksUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/api/orders").hasAuthority("SCOPE_USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/my-orders").hasAuthority("SCOPE_USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/{orderId}").hasAuthority("SCOPE_USER")
                        .pathMatchers(HttpMethod.DELETE, "/api/orders/{orderId}/cancellation").hasAuthority("SCOPE_USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/merchant").hasAuthority("SCOPE_MERCHANT")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));

        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}