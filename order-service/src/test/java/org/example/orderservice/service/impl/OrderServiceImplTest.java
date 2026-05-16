package org.example.orderservice.service.impl;

import org.example.orderservice.dto.NotificationMessage;
import org.example.orderservice.entity.Address;
import org.example.orderservice.entity.Book;
import org.example.orderservice.entity.Order;
import org.example.orderservice.publisher.NotificationPublisher;
import org.example.orderservice.repository.AddressRepository;
import org.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private Order testOrder2;
    private Book testBook;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setProductId(1L);
        testBook.setProductName("Clean Code");
        testBook.setCoverImageUrl("http://example.com/cover.jpg");

        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setUserId(100L);
        testOrder.setBook(testBook);
        testOrder.setQuantity(2);
        testOrder.setAmountPaid(100.00);
        testOrder.setCartId(200L);

        testOrder2 = new Order();
        testOrder2.setOrderId(2L);
        testOrder2.setUserId(101L);
        testOrder2.setBook(testBook);
        testOrder2.setQuantity(1);
        testOrder2.setAmountPaid(50.00);

        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setCustomerId(100L);
        testAddress.setFlatNumber("123");
        testAddress.setCity("New York");
        testAddress.setState("NY");
        testAddress.setPincode(10001);
        testAddress.setFullName("John Doe");
        testAddress.setMobileNumber("1234567890");
    }

    // Tests for getAllOrders
    @Test
    void getAllOrders_shouldReturnAllOrders() {
        List<Order> orders = Arrays.asList(testOrder, testOrder2);
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getAllOrders_shouldReturnEmptyListWhenNoOrders() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList());

        List<Order> result = orderService.getAllOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository, times(1)).findAll();
    }

    // Tests for getOrdersByUserId
    @Test
    void getOrdersByUserId_shouldReturnOrdersForUser() {
        List<Order> userOrders = Arrays.asList(testOrder);
        when(orderRepository.findByUserId(100L)).thenReturn(userOrders);

        List<Order> result = orderService.getOrdersByUserId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getUserId());
        verify(orderRepository, times(1)).findByUserId(100L);
    }

    @Test
    void getOrdersByUserId_shouldReturnEmptyListWhenUserHasNoOrders() {
        when(orderRepository.findByUserId(999L)).thenReturn(Arrays.asList());

        List<Order> result = orderService.getOrdersByUserId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Tests for placeOrder
    @Test
    void placeOrder_shouldPlaceOrderWithCODPaymentByDefault() {
        Order newOrder = new Order();
        newOrder.setUserId(100L);
        newOrder.setBook(testBook);
        newOrder.setQuantity(2);
        newOrder.setAmountPaid(100.00);

        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("Cart service unavailable"));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        Order result = orderService.placeOrder(newOrder);

        assertNotNull(result);
        assertEquals("COD", newOrder.getModeOfPayment());
        assertEquals("PLACED", newOrder.getOrderStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void placeOrder_shouldPublishNotificationAfterPlacingOrder() {
        Order newOrder = new Order();
        newOrder.setUserId(100L);
        newOrder.setOrderId(1L);
        newOrder.setBook(testBook);
        newOrder.setQuantity(1);
        newOrder.setAmountPaid(50.00);

        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("Cart service unavailable"));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);
        doNothing().when(notificationPublisher).publish(any(NotificationMessage.class));

        Order result = orderService.placeOrder(newOrder);

        assertNotNull(result);
        verify(notificationPublisher, times(1)).publish(any(NotificationMessage.class));
    }

    @Test
    void placeOrder_shouldNotFailWhenNotificationPublishFails() {
        Order newOrder = new Order();
        newOrder.setUserId(100L);
        newOrder.setBook(testBook);
        newOrder.setQuantity(1);
        newOrder.setAmountPaid(50.00);

        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("Cart service unavailable"));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);
        doThrow(new RuntimeException("Notification service down"))
                .when(notificationPublisher).publish(any(NotificationMessage.class));

        Order result = orderService.placeOrder(newOrder);

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void placeOrder_shouldDeductStockWhenPlacingOrder() {
        Order newOrder = new Order();
        newOrder.setUserId(100L);
        newOrder.setOrderId(1L);
        newOrder.setBook(testBook);
        newOrder.setQuantity(2);
        newOrder.setAmountPaid(100.00);

        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("Cart service unavailable"));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);
        doNothing().when(restTemplate).put(anyString(), any());
        doNothing().when(notificationPublisher).publish(any(NotificationMessage.class));

        Order result = orderService.placeOrder(newOrder);

        assertNotNull(result);
        verify(restTemplate).put(contains("deduct-stock"), any());
    }

    // Tests for onlinePayment
    @Test
    void onlinePayment_shouldMarkOrderAsPaidWithOnlineMode() {
        Order paymentOrder = new Order();
        paymentOrder.setOrderId(1L);
        paymentOrder.setUserId(100L);

        when(orderRepository.save(any(Order.class))).thenReturn(paymentOrder);
        doNothing().when(notificationPublisher).publish(any(NotificationMessage.class));

        Order result = orderService.onlinePayment(paymentOrder);

        assertNotNull(result);
        assertEquals("ONLINE", paymentOrder.getModeOfPayment());
        assertEquals("PAID", paymentOrder.getOrderStatus());
        assertNotNull(paymentOrder.getOrderDate());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void onlinePayment_shouldPublishNotificationAfterPayment() {
        Order paymentOrder = new Order();
        paymentOrder.setOrderId(1L);
        paymentOrder.setUserId(100L);

        when(orderRepository.save(any(Order.class))).thenReturn(paymentOrder);
        doNothing().when(notificationPublisher).publish(any(NotificationMessage.class));

        Order result = orderService.onlinePayment(paymentOrder);

        verify(notificationPublisher, times(1)).publish(any(NotificationMessage.class));
    }

    // Tests for changeOrderStatus
    @Test
    void changeOrderStatus_shouldChangeStatusToConfirmed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        String result = orderService.changeOrderStatus(1L, "CONFIRMED");

        assertEquals("Status Updated", result);
        assertEquals("CONFIRMED", testOrder.getOrderStatus());
        assertNotNull(testOrder.getConfirmedDate());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void changeOrderStatus_shouldChangeStatusToDispatched() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        String result = orderService.changeOrderStatus(1L, "DISPATCHED");

        assertEquals("Status Updated", result);
        assertEquals("DISPATCHED", testOrder.getOrderStatus());
        assertNotNull(testOrder.getDispatchedDate());
    }

    @Test
    void changeOrderStatus_shouldChangeStatusToDelivered() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        String result = orderService.changeOrderStatus(1L, "DELIVERED");

        assertEquals("Status Updated", result);
        assertEquals("DELIVERED", testOrder.getOrderStatus());
        assertNotNull(testOrder.getDeliveredDate());
    }

    @Test
    void changeOrderStatus_shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.changeOrderStatus(99L, "CONFIRMED")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // Tests for deleteOrder
    @Test
    void deleteOrder_shouldDeleteOrder() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(restTemplate).put(anyString(), any());

        orderService.deleteOrder(1L);

        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteOrder_shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.deleteOrder(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(orderRepository, never()).deleteById(99L);
    }

    @Test
    void deleteOrder_shouldRestoreStockWhenDeletingOrder() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(restTemplate).put(anyString(), any());

        orderService.deleteOrder(1L);

        verify(restTemplate).put(contains("restore-stock"), any());
        verify(orderRepository, times(1)).deleteById(1L);
    }

    // Tests for getOrderById
    @Test
    void getOrderById_shouldReturnOrderWhenFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        Order result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_shouldThrowExceptionWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.getOrderById(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // Tests for storeAddress
    @Test
    void storeAddress_shouldSaveAddressSuccessfully() {
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        orderService.storeAddress(testAddress);

        verify(addressRepository, times(1)).save(testAddress);
    }

    @Test
    void storeAddress_shouldStoreAddressWithAllDetails() {
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.storeAddress(testAddress);

        verify(addressRepository).save(argThat(addr ->
                addr.getCustomerId().equals(100L) &&
                        addr.getFlatNumber().equals("123") &&
                        addr.getCity().equals("New York")
        ));
    }
}
