package com.loqal.paymentservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
//@EnableJpaRepositories(basePackages = "com.example.paymentservice.repository")
public class TenantConfig {
//    @Bean
//    public OncePerRequestFilter tenantFilter() {
//        return new OncePerRequestFilter() {
//            @Override
//            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//                String tenantId = request.getHeader("X-Tenant-Id");
//                TenantContextHolder.setTenantId(tenantId);
//                try {
//                    filterChain.doFilter(request, response);
//                } finally {
//                    TenantContextHolder.clear();
//                }
//            }
//        };
//    }
}