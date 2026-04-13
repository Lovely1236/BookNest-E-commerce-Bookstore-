package org.example.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.entity.Address;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.AddressRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.dto.NotificationMessage;
import org.example.orderservice.publisher.NotificationPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final NotificationPublisher notificationPublisher;

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
        order.setOrderDate(LocalDate.now());
        if (order.getModeOfPayment() == null || order.getModeOfPayment().isBlank()) {
            order.setModeOfPayment("COD");
        }
        order.setOrderStatus("PLACED");
        Order saved = orderRepository.save(order);

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
        orderRepository.save(order);
        return "Status Updated";
    }

    @Override
    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id " + orderId);
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
}
