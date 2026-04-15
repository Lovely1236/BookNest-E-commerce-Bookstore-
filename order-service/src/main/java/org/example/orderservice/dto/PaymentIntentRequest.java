package org.example.orderservice.dto;

import lombok.Data;

@Data
public class PaymentIntentRequest {
    private Long orderId;
    private double amount;
    private String currency;
}
