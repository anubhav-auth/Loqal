// In a shared DTO module or in the OrderService
package com.Loqal.orderservice.dto.events;

import com.Loqal.orderservice.dto.ProductOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancellationEvent {
    private UUID orderId;
    private List<ProductOrderRequest> items;
}