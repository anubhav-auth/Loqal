package com.loqal.paymentservice.entity.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Integer amount;
    private String currency;
    private String receipt;
}