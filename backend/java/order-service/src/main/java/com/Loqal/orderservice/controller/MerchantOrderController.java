package com.Loqal.orderservice.controller;

import com.Loqal.orderservice.entity.Order;
import com.Loqal.OrderService.repository.OrderRepository;
import com.Loqal.OrderService.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
    /**
     * Controller for merchant-related order queries.
     * Merchants can view all orders directed to them with item and quantity details.
     */
    @RestController
    @RequestMapping("/merchant")
        public class MerchantOrderController {
        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private OrderItemRepository orderItemRepository;


        @GetMapping("/{merchantId}/orders")
        public Map<UUID, List<OrderItem>> getOrdersForMerchant(@PathVariable UUID merchantId) {
            List<Order> orders = orderRepository.findAll()
                    .stream()
                    .filter(o -> merchantId.equals(o.getMerchantId()))
                    .collect(Collectors.toList());

            Map<UUID, List<OrderItem>> merchantOrders = new HashMap<>();
            for (Order order : orders) {
                List<OrderItem> items = orderItemRepository.findAll()
                        .stream()
                        .filter(item -> order.getId().equals(item.getOrderId()))
                        .collect(Collectors.toList());
                merchantOrders.put(order.getId(), items);
            }
            return merchantOrders;
        }
    }
}
