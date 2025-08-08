package com.loqal.merchantservice;

import com.loqal.merchantservice.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class MerchantService {
    public MerchantDto getMerchantProfile(String merchantId) {
    }

    public MerchantDto updateMerchantProfile(String merchantId, MerchantDto merchantDto) {
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
