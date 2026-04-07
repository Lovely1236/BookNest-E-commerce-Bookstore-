package org.example.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.entity.Address;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.AddressRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
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
        return orderRepository.save(order);
    }

    @Override
    public Order onlinePayment(Order order) {
        order.setOrderDate(LocalDate.now());
        order.setModeOfPayment("ONLINE");
        order.setOrderStatus("PAID");
        return orderRepository.save(order);
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
