package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.entity.Order;
import com.Loqal.orderservice.dto.OrderRequest;
import com.Loqal.orderservice.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
public class MerchantController {

    @Autowired
    private OrderRepository orderRepository;

    @Hidden
    @PostMapping("/merchant/fetch")
    public List<OrderRequest> fetchMerchantOrderHistory(HttpServletRequest request) {
        String jwt = request.getHeader("Authorization");
        UUID merchantId = JwtUtils.extractTenantId(jwt); // You must implement this JWT utility

        List<Order> orders = orderRepository.findByMerchantId(merchantId);

        // Map entity to DTO, following your existing OrderRequest fields
        return orders.stream().map(order -> {
            OrderRequest dto = new OrderRequest();
            dto.setCustomerId(order.getCustomerId());
            dto.setMerchantId(order.getMerchantId());
            dto.setItemsOrdered(order.getItemsOrdered());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setDiscountAmount(order.getDiscountAmount());
            dto.setFinalAmount(order.getFinalAmount());
            dto.setPaymentStatus(order.getPaymentStatus());
            dto.setDeliveryAddressId(order.getDeliveryAddressId());
            dto.setCurrentStatus(order.getCurrentStatus());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setUpdatedAt(order.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}