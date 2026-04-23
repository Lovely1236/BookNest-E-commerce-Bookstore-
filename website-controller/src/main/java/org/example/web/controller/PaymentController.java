package org.example.web.controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.example.web.service.CurrentUserService;
import org.example.web.service.StorefrontUser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @Value("${stripe.secret:}")
    private String stripeSecret;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    @Value("${website.url:http://localhost:8090}")
    private String websiteUrl;

    public Map<String, Object> createCheckoutSession(HttpSession session) throws Exception {
        StorefrontUser user = currentUserService.currentUser(session);
        if (!user.authenticated()) {
            return Map.of("error", "authentication required");
        }

        Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + user.userId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.getOrDefault("items", List.of());

        Stripe.apiKey = stripeSecret;

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(websiteUrl + "/payment/success")
                .setCancelUrl(websiteUrl + "/payment/cancel");

        // attach userId so webhook can create orders after successful payment
        paramsBuilder.putMetadata("userId", String.valueOf(user.userId()));

        for (Map<String, Object> item : items) {
            long unitAmount = (long) (Double.parseDouble(String.valueOf(item.getOrDefault("price", 0))) * 100);
            paramsBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("inr")
                            .setUnitAmount(unitAmount)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(String.valueOf(item.getOrDefault("bookTitle", "Book")))
                                    .build())
                            .build())
                    .setQuantity(((Number) item.getOrDefault("quantity", 1)).longValue())
                    .build());
        }

        SessionCreateParams params = paramsBuilder.build();

        Session sessionObj = Session.create(params);

        return Map.of("id", sessionObj.getId());
    }

    public String handleWebhook(HttpServletRequest request) throws Exception {
        String payload = new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String sigHeader = request.getHeader("Stripe-Signature");

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException ex) {
            return "invalid signature";
        }

        if ("checkout.session.completed".equals(event.getType())) {
            // deserialize session
            Session sessionObj = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (sessionObj != null && "paid".equals(sessionObj.getPaymentStatus())) {
                String userId = sessionObj.getMetadata().get("userId");
                if (userId != null && !userId.isBlank()) {
                    Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + userId);
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) cart.getOrDefault("items", java.util.List.of());
                    for (Map<String, Object> item : items) {
                        double lineAmount = toDouble(item.get("price")) * toDouble(item.get("quantity"));
                        Map<String, Object> order = buildOrderPayload(Long.parseLong(userId), item, lineAmount);
                        backendGateway.postStrict(backendGateway.serviceUrls().order() + "/orders/online", order);
                    }
                    backendGateway.deleteStrict(backendGateway.serviceUrls().cart() + "/cart/" + userId + "/clear");
                }
            }
        }

        return "ok";
    }

    private Map<String, Object> buildOrderPayload(Long userId, Map<String, Object> item, double amountPaid) {
        Map<String, Object> order = new java.util.LinkedHashMap<>();
        order.put("userId", userId);
        order.put("quantity", (int) toDouble(item.get("quantity")));
        order.put("amountPaid", amountPaid);
        order.put("address", Map.of(
                "customerId", userId,
                "fullName", "", // user profile can be fetched by backend if needed
                "mobileNumber", "",
                "flatNumber", "221B",
                "city", "Lucknow",
                "pincode", 226001,
                "state", "UP"));
        order.put("book", Map.of(
                "productId", toLong(item.get("bookId")),
                "productName", String.valueOf(item.getOrDefault("bookTitle", "BookNest Pick"))));
        return order;
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return 0.0;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }
}
