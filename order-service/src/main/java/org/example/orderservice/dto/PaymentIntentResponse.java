package org.example.orderservice.dto;

import lombok.Data;

@Data
public class PaymentIntentResponse {
    private String clientSecret;
    private Long orderId;
    private double amount;
    private String currency;

    public PaymentIntentResponse(String clientSecret, Long orderId, double amount, String currency) {
        this.clientSecret = clientSecret;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
    }
}
