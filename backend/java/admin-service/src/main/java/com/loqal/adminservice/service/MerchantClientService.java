package com.loqal.adminservice.service;

import com.loqal.adminservice.entity.Merchant;
import com.loqal.adminservice.entity.dto.MerchantDTO;
import com.loqal.adminservice.entity.dto.MerchantResponseDTO;
import com.loqal.adminservice.entity.dto.Status;
import com.loqal.adminservice.entity.dto.UserProfileDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantClientService {
    private final WebClient webClient;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${services.merchant-service.url}")
    private String merchantUri;


    @Transactional
    public Merchant onboardMerchantFromUser(UUID userId, MerchantDTO merchantDTO, String serviceJwt) {
        String userServiceUri = userServiceUrl + "/users/profile/" + userId.toString();
        UserProfileDto userProfile = webClient.get()
                .uri(userServiceUri)
                .retrieve()
                .bodyToMono(UserProfileDto.class)
                .block();

        if (userProfile == null) {
            throw new RuntimeException("User not found for ID: " + userId);
        }

        Merchant request = new Merchant();
        request.setUserId(userId);
        request.setTenantId(userProfile.getTenantId());
        request.setName(userProfile.getFullName());
        request.setBusinessType(merchantDTO.getBusinessType());
        request.setBusinessName(merchantDTO.getBusinessName());
        request.setTaxId(merchantDTO.getTaxId());
        request.setDescription(merchantDTO.getDescription());
        request.setAddress(merchantDTO.getAddress());
        request.setPhoneNumber(userProfile.getPhoneNumber());
        request.setEmail(userProfile.getEmail());
        request.setLogoUrl(merchantDTO.getLogoUrl());
        request.setStatus(Status.PENDING_APPROVAL);

        String merchantServiceUri = merchantUri + "/internal/merchants/onboard";
        return webClient.post()
                .uri(merchantServiceUri)
                .header("Authorization", "Bearer " + serviceJwt)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Merchant.class)
                .block();
    }


    @Transactional
    public Merchant onboardMerchant(Merchant merchant, String serviceJwt) {

        merchant.setUserId(UUID.randomUUID());
        merchant.setStatus(Status.PENDING_APPROVAL);

        String merchantServiceUri = merchantUri + "/internal/merchants/onboard";
        return webClient.post()
                .uri(merchantServiceUri)
                .header("Authorization", "Bearer " + serviceJwt)
                .bodyValue(merchant)
                .retrieve()
                .bodyToMono(Merchant.class)
                .block();
    }


}