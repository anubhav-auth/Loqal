package com.loqal.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.jwks-uri}")
    private String jwksUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Anyone with a valid token can view products
                        .requestMatchers(HttpMethod.GET, "/product/**").authenticated()
                        // Only allow services with 'internal-service' authority to access this endpoint.
                        .requestMatchers("/api/products/internal/**").hasAuthority("SCOPE_internal-service")
                        // Only MERCHANTS can create or update products
                        .requestMatchers(HttpMethod.POST, "/product").hasAuthority("SCOPE_MERCHANT")
                        .requestMatchers(HttpMethod.PUT, "/product/**").hasAuthority("SCOPE_MERCHANT")
                        // Internal stock updates also require a valid token (service token)
                        .requestMatchers("/internal/**").authenticated()
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}