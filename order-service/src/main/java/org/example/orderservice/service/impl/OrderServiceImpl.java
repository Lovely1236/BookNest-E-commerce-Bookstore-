package org.example.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.entity.Address;
import org.example.orderservice.entity.Book;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.AddressRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.dto.NotificationMessage;
import org.example.orderservice.publisher.NotificationPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final NotificationPublisher notificationPublisher;
    private final RestTemplate restTemplate;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Order placeOrder(Order order) {
        enrichOrderFromCart(order);

        order.setOrderDate(LocalDate.now());
        if (order.getModeOfPayment() == null || order.getModeOfPayment().isBlank()) {
            order.setModeOfPayment("COD");
        }
        order.setOrderStatus("PLACED");
        
        // Log the order details for debugging
        System.out.println("Placing order - amountPaid: " + order.getAmountPaid() + ", cartId: " + order.getCartId());
        
        Order saved = orderRepository.save(order);
        
        // Deduct stock from book
        try {
            if (order.getBook() != null && order.getBook().getProductId() != null && order.getQuantity() > 0) {
                String url = "http://book-service:8081/books/" + order.getBook().getProductId() + "/deduct-stock?quantity=" + order.getQuantity();
                restTemplate.put(url, null);
                System.out.println("Stock deducted for book: " + order.getBook().getProductId() + ", quantity: " + order.getQuantity());
            }
        } catch (Exception e) {
            System.err.println("Failed to deduct stock: " + e.getMessage());
            // Don't fail order if stock deduction fails - log and continue
        }

        // publish notification
        try {
            NotificationMessage msg = new NotificationMessage();
            msg.setUserId(saved.getUserId());
            msg.setType("ORDER_PLACED");
            msg.setMessage("Your order " + saved.getOrderId() + " has been placed.");
            notificationPublisher.publish(msg);
        } catch (Exception e) {
            // logging omitted — don't fail order on notification publish
            System.err.println("Failed to publish notification: " + e.getMessage());
        }

        return saved;
    }

    @Override
    public Order onlinePayment(Order order) {
        order.setOrderDate(LocalDate.now());
        order.setModeOfPayment("ONLINE");
        order.setOrderStatus("PAID");
        Order saved = orderRepository.save(order);
        try {
            NotificationMessage msg = new NotificationMessage();
            msg.setUserId(saved.getUserId());
            msg.setType("ORDER_PAID");
            msg.setMessage("Your payment for order " + saved.getOrderId() + " was successful.");
            notificationPublisher.publish(msg);
        } catch (Exception e) {
            System.err.println("Failed to publish notification: " + e.getMessage());
        }
        return saved;
    }

    @Override
    public String changeOrderStatus(Long orderId, String status) {
        Order order = getExistingOrder(orderId);
        order.setOrderStatus(status);
        
        // Set the appropriate date based on status
        if ("CONFIRMED".equals(status)) {
            order.setConfirmedDate(LocalDate.now());
        } else if ("DISPATCHED".equals(status)) {
            order.setDispatchedDate(LocalDate.now());
        } else if ("DELIVERED".equals(status)) {
            order.setDeliveredDate(LocalDate.now());
        }
        
        orderRepository.save(order);
        return "Status Updated";
    }

    @Override
    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id " + orderId);
        }
        
        // Get the order to access book information for stock restoration
        Order order = getExistingOrder(orderId);
        
        // Restore stock when order is cancelled
        try {
            if (order.getBook() != null && order.getBook().getProductId() != null && order.getQuantity() > 0) {
                String url = "http://book-service:8081/books/" + order.getBook().getProductId() + "/restore-stock?quantity=" + order.getQuantity();
                restTemplate.put(url, null);
                System.out.println("Stock restored for book: " + order.getBook().getProductId() + ", quantity: " + order.getQuantity());
            }
        } catch (Exception e) {
            System.err.println("Failed to restore stock: " + e.getMessage());
            // Log but don't fail - still delete the order
        }
        
        orderRepository.deleteById(orderId);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return getExistingOrder(orderId);
    }

    @Override
    public void storeAddress(Address address) {
        addressRepository.save(address);
    }

    private Order getExistingOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found with id " + orderId));
    }

    @SuppressWarnings("unchecked")
    private void enrichOrderFromCart(Order order) {
        if (order == null || order.getUserId() == null) {
            return;
        }

        try {
            Map<String, Object> cart = restTemplate.getForObject(
                    "http://cart-service:8083/cart/" + order.getUserId(), Map.class);

            if (cart == null) {
                return;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
            if (items == null || items.isEmpty()) {
                System.out.println("Order enrichment skipped: cart has no items for user " + order.getUserId());
                return;
            }
            System.out.println("Order enrichment: found " + items.size() + " cart item(s) for user " + order.getUserId());

            Map<String, Object> firstItem = items.get(0);
            Book embeddedBook = new Book();

            Number productId = (Number) firstItem.get("bookId");
            if (productId != null) {
                embeddedBook.setProductId(productId.longValue());
            }

            embeddedBook.setProductName((String) firstItem.get("bookTitle"));
            embeddedBook.setCoverImageUrl((String) firstItem.get("bookCoverUrl"));

            // Fallback: resolve missing title/cover from book-service through gateway
            if ((embeddedBook.getProductName() == null || embeddedBook.getProductName().isBlank()
                    || embeddedBook.getCoverImageUrl() == null || embeddedBook.getCoverImageUrl().isBlank())
                    && embeddedBook.getProductId() != null) {
                try {
                    Map<String, Object> book = restTemplate.getForObject(
                            "http://api-gateway:8080/api/book/books/" + embeddedBook.getProductId(), Map.class);
                    if (book != null) {
                        if (embeddedBook.getProductName() == null || embeddedBook.getProductName().isBlank()) {
                            embeddedBook.setProductName((String) book.get("title"));
                        }
                        if (embeddedBook.getCoverImageUrl() == null || embeddedBook.getCoverImageUrl().isBlank()) {
                            embeddedBook.setCoverImageUrl((String) book.get("coverImageUrl"));
                        }
                    }
                } catch (Exception ignored) {
                    // Keep order placement resilient even if enrichment fallback fails
                }
            }

            if (order.getBook() == null) {
                order.setBook(embeddedBook);
            }

            if (order.getQuantity() <= 0) {
                int totalQty = items.stream()
                        .map(i -> (Number) i.get("quantity"))
                        .filter(q -> q != null)
                        .mapToInt(Number::intValue)
                        .sum();
                order.setQuantity(totalQty > 0 ? totalQty : 1);
            }

            if (order.getAmountPaid() <= 0) {
                Number totalPrice = (Number) cart.get("totalPrice");
                if (totalPrice != null) {
                    order.setAmountPaid(totalPrice.doubleValue());
                }
            }

            System.out.println("Order enrichment result - productId: "
                    + (order.getBook() != null ? order.getBook().getProductId() : null)
                    + ", productName: "
                    + (order.getBook() != null ? order.getBook().getProductName() : null)
                    + ", quantity: " + order.getQuantity());
        } catch (Exception e) {
            System.err.println("Failed to enrich order from cart: " + e.getMessage());
        }
    }
}
