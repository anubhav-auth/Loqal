package com.loqal.adminservice.service;

import com.loqal.adminservice.entity.dto.MerchantRequestDTO;
import com.loqal.adminservice.entity.dto.MerchantResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MerchantService {
    private final WebClient webClient;

    public MerchantResponseDTO onboardMerchant(MerchantRequestDTO request) {
        String merchantServiceUrl = "http://merchant-service.default.svc.cluster.local/merchants";
        return webClient.post()
                .uri(merchantServiceUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MerchantResponseDTO.class)
                .block(); // Simplified; add Resilience4j retries
    }
}