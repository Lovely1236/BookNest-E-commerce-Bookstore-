package org.example.orderservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.dto.PaymentIntentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    public PaymentIntentResponse createPaymentIntent(Long orderId, double amount, String currency) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        Map<String, Object> params = new HashMap<>();
        params.put("amount", (long) (amount * 100)); // Stripe expects amount in smallest unit (cents)
        params.put("currency", currency.toLowerCase());
        params.put("metadata", Map.of("orderId", orderId.toString()));

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("Created PaymentIntent {} for order {}", paymentIntent.getId(), orderId);

        return new PaymentIntentResponse(
            paymentIntent.getClientSecret(),
            orderId,
            amount,
            currency
        );
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public boolean isPaymentSuccessful(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId);
        return "succeeded".equals(paymentIntent.getStatus());
    }
}
