package com.loqal.merchantservice;

import com.loqal.merchantservice.dto.*;
import com.loqal.merchantservice.entity.MerchantExtended;
import com.loqal.merchantservice.repository.MerchantExtendedRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class MerchantService {

    private final MerchantExtendedRepository merchantExtendedRepository;

    @Autowired
    public MerchantService(MerchantExtendedRepository merchantExtendedRepository) {
        this.merchantExtendedRepository = merchantExtendedRepository;
    }

    public MerchantExtended getMerchantProfile(UUID merchantId) {
        return merchantExtendedRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found with ID: " + merchantId));
    }

    @Transactional
    public UUID save(MerchantExtendedDto merchantExtendedDto, UUID userId) {
        MerchantExtended merchantExtended = new MerchantExtended();

        merchantExtended.setUserId(userId);
        merchantExtended.setName(merchantExtendedDto.name());
        merchantExtended.setDescription(merchantExtendedDto.description());
        merchantExtended.setAddress(merchantExtendedDto.address());
        merchantExtended.setLogoUrl(merchantExtendedDto.logoUrl());

        MerchantExtended saved = merchantExtendedRepository.save(merchantExtended);
        return saved.getId();
    }

    public MerchantExtendedDto updateMerchantProfile(UUID tenant_id, MerchantExtendedDto merchantExtendedDto) {
        MerchantExtended merchantExtended = merchantExtendedRepository.findById(tenant_id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found with ID: " + tenant_id));
        merchantExtended.setName(merchantExtendedDto.name());
        merchantExtended.setDescription(merchantExtendedDto.description());
        merchantExtended.setAddress(merchantExtendedDto.address());
        merchantExtended.setLogoUrl(merchantExtendedDto.logoUrl());
        MerchantExtended updated = merchantExtendedRepository.save(merchantExtended);
        return new MerchantExtendedDto(
                updated.getName(),
                updated.getDescription(),
                updated.getAddress(),
                updated.getLogoUrl()
        );
    }

    public List<ProductDto> getProductsForMerchant(String merchantId) {
        return null;
    }

    public ProductDto createProductForMerchant(String merchantId, ProductDto productDto) {
    }

    public void updateInventory(String merchantId, String productId, UpdateStockRequestDto stockRequest) {
    }

    public List<OrderDto> getOrdersForMerchant(String merchantId, String status) {
    }

    public OrderDto updateOrderStatus(String merchantId, String orderId, UpdateStatusRequestDto statusRequest) {
    }

    public AnalyticsDto getSalesAnalyticsForMerchant(String merchantId) {
        return null;
    }
}
