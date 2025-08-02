package com.Loqal.productservice.dto.events;

import com.Loqal.productservice.entity.ProductOrderRequest;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class OrderCreationRequest {
    private UUID orderId;
    private List<ProductOrderRequest> items;
}