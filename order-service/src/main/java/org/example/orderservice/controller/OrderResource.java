package org.example.orderservice.controller;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.ConfirmPaymentRequest;
import org.example.orderservice.dto.PaymentIntentResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderResource {

    private final OrderService orderService;
    private final StripeService stripeService;

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/user/{userId}")
    public List<Order> getByUser(@PathVariable Long userId) {
        return orderService.getOrdersByUserId(userId);
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping("/place")
    public Order placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    @PostMapping("/online")
    public Order onlinePayment(@RequestBody Order order) {
        return orderService.onlinePayment(order);
    }

    @PutMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        return orderService.changeOrderStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return "Deleted";
    }

    @PostMapping("/{orderId}/payment-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }

            PaymentIntentResponse response = stripeService.createPaymentIntent(
                orderId,
                order.getAmountPaid(),
                "INR"
            );

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{orderId}/confirm-payment")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @PathVariable Long orderId,
            @RequestBody ConfirmPaymentRequest request) {
        try {
            boolean isSuccessful = stripeService.isPaymentSuccessful(request.getPaymentIntentId());

            if (isSuccessful) {
                // Update order status to CONFIRMED
                orderService.changeOrderStatus(orderId, "CONFIRMED");

                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Payment successful and order confirmed");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("status", "failed");
                response.put("message", "Payment not successful");
                return ResponseEntity.status(400).body(response);
            }
        } catch (StripeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
