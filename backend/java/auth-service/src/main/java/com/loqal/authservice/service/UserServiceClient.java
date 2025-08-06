package com.loqal.authservice.service;

import com.loqal.authservice.entity.dto.UserInfoDto;
import com.loqal.authservice.entity.dto.UserOauthRegisterDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserServiceClient {
    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder builder,
                             @Value("${services.user-service.url}") String userServiceUrl) {
        this.webClient = builder.baseUrl(userServiceUrl).build();
    }

    public Mono<UserInfoDto> registerOrFetchUser(UserOauthRegisterDto dto, String serviceJwt) {
        return webClient.post()
                .uri("/internal/users/oauth-register")
                .header("Authorization", "Bearer " + serviceJwt)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(UserInfoDto.class); // 👈 Return the Mono directly
    }
}

