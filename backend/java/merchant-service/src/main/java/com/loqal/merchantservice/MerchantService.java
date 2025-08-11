package com.loqal.merchantservice;

import com.loqal.merchantservice.dto.*;
import com.loqal.merchantservice.entity.MerchantExtended;
import com.loqal.merchantservice.entity.Order;
import com.loqal.merchantservice.repository.MerchantExtendedRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;


@Service
public class MerchantService {

    private final MerchantExtendedRepository merchantExtendedRepository;
    private final WebClient productServiceWebClient;
    private final WebClient orderServiceWebClient;

    @Autowired
    public MerchantService(
            MerchantExtendedRepository merchantExtendedRepository,
            @Qualifier("productServiceWebClient") WebClient productServiceWebClient,
            @Qualifier("orderServiceWebClient") WebClient orderServiceWebClient
    ) {
        this.merchantExtendedRepository = merchantExtendedRepository;
        this.productServiceWebClient = productServiceWebClient;
        this.orderServiceWebClient = orderServiceWebClient;
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

        return productServiceWebClient.get()
                .uri("/products/merchant")
                .retrieve()
                .bodyToFlux(ProductDto.class)
                .collectList()
                .doOnError(error ->
                        System.err.println("Failed to fetch products: " + error.getMessage())
                )
                .block();
    }

    public ProductDto createProductForMerchant(String merchantId, ProductDto productDto) {
        return productServiceWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{merchantId}")
                        .build(merchantId))
                .bodyValue(productDto)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .block();
    }

    public ProductDto updateInventory(String merchantId, String productId, UpdateStockRequestDto stockRequest) {
        return productServiceWebClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{productId}/{merchantId}")
                        .build(productId, merchantId))
                .bodyValue(stockRequest)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .doOnError(error ->
                        System.err.println("Failed to update inventory: " + error.getMessage())
                )
                .block();
    }

    public void deleteInventoryItem(String merchantId, String productId) {
        productServiceWebClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{productId}/{merchantId}")
                        .build(productId, merchantId))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error ->
                        System.err.println("Failed to update inventory: " + error.getMessage())
                )
                .block();
    }


    public List<Order> getOrdersForMerchant(String merchantId) {
        return orderServiceWebClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/orders/merchant/{merchantId}")
                                .build(merchantId)
                )
                .retrieve()
                .bodyToFlux(Order.class)
                .doOnError(error ->
                        System.err.println("Failed to fetch orders: " + error.getMessage())
                )
                .collectList()
                .block();
    }

    public OrderDto updateOrderStatus(String merchantId, String orderId, UpdateStatusRequestDto statusRequest) {
        return null;
    }

    public AnalyticsDto getSalesAnalyticsForMerchant(String merchantId) {
        return null;
    }
}
