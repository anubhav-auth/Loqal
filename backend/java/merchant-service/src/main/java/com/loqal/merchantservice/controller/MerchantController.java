package com.loqal.merchantservice.controller;

import com.loqal.merchantservice.MerchantService;
import com.loqal.merchantservice.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantDto> getMerchantById(@PathVariable String merchantId) {
        MerchantDto merchant = merchantService.getMerchantProfile(merchantId);
        return ResponseEntity.ok(merchant);
    }


    @PutMapping("/{merchantId}")
    public ResponseEntity<MerchantDto> updateMerchant(@PathVariable String merchantId, @RequestBody MerchantDto merchantDto) {
        MerchantDto updatedMerchant = merchantService.updateMerchantProfile(merchantId, merchantDto);
        return ResponseEntity.ok(updatedMerchant);
    }


    @GetMapping("/{merchantId}/products")
    public ResponseEntity<List<ProductDto>> getProductsByMerchant(@PathVariable String merchantId) {
        List<ProductDto> products = merchantService.getProductsForMerchant(merchantId);
        return ResponseEntity.ok(products);
    }


    @PostMapping("/{merchantId}/products")
    public ResponseEntity<ProductDto> createProductForMerchant(@PathVariable String merchantId, @RequestBody ProductDto productDto) {
        ProductDto newProduct = merchantService.createProductForMerchant(merchantId, productDto);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }


    @PutMapping("/{merchantId}/inventory/{productId}")
    public ResponseEntity<ProductDto> updateInventoryForProduct(
            @PathVariable String merchantId,
            @PathVariable String productId,
            @RequestBody UpdateStockRequestDto stockRequest) {
        ProductDto productDto = merchantService.updateInventory(merchantId, productId, stockRequest);
        if (productDto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(productDto);
    }


    @GetMapping("/{merchantId}/orders")
    public ResponseEntity<List<OrderDto>> getOrdersByMerchant(@PathVariable String merchantId, @RequestParam(required = false) String status) {
        List<OrderDto> orders = merchantService.getOrdersForMerchant(merchantId, status);
        return ResponseEntity.ok(orders);
    }


    @PutMapping("/{merchantId}/orders/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable String merchantId,
            @PathVariable String orderId,
            @RequestBody UpdateStatusRequestDto statusRequest) {
        OrderDto updatedOrder = merchantService.updateOrderStatus(merchantId, orderId, statusRequest);
        return ResponseEntity.ok(updatedOrder);
    }


    @GetMapping("/{merchantId}/analytics/sales")
    public ResponseEntity<AnalyticsDto> getSalesAnalytics(@PathVariable String merchantId) {
        AnalyticsDto analytics = merchantService.getSalesAnalyticsForMerchant(merchantId);
        return ResponseEntity.ok(analytics);
    }
}