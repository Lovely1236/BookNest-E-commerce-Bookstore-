package org.example.orderservice.dto;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private String paymentIntentId;
}
