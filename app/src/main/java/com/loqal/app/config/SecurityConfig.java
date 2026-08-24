package com.loqal.app.config;

import com.loqal.identity.auth.utils.RSAKeyProvider;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.security.interfaces.RSAPublicKey;

/**
 * Unified security chain for the whole monolith (PRD §7.5, §9).
 * Role/SCOPE-based path tightening is deferred; everything non-public
 * requires a valid RS256 token verified against the persisted key.
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RSAKeyProvider keyProvider;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
                rolesConverter = new org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter();
        rolesConverter.setPrincipalClaimName("user_id");
        rolesConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null || roles.isEmpty()) {
                return reactor.core.publisher.Flux.empty();
            }
            return reactor.core.publisher.Flux.fromIterable(roles)
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
        });

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/**", "/.well-known/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/custom-docs/**", "/custom-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        // Public storefront browsing
                        .pathMatchers(HttpMethod.GET, "/products/public/**").permitAll()
                        // Admin-only platform management (PRD §8.3)
                        .pathMatchers("/platform/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    jwt.jwtDecoder(jwtDecoder());
                    jwt.jwtAuthenticationConverter(rolesConverter);
                }))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        try {
            RSAPublicKey publicKey = keyProvider.getPublicKey();
            return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to build JWT decoder from configured RSA public key", e);
        }
    }
}
