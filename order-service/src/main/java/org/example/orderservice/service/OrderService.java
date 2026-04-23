package org.example.orderservice.service;

import org.example.orderservice.entity.Address;
import org.example.orderservice.entity.Order;

import java.util.List;

public interface OrderService {

    List<Order> getAllOrders();

    List<Order> getOrdersByUserId(Long userId);

    Order placeOrder(Order order);

    Order onlinePayment(Order order);

    String changeOrderStatus(Long orderId, String status);

    void deleteOrder(Long orderId);

    Order getOrderById(Long orderId);

    void storeAddress(Address address);
}
