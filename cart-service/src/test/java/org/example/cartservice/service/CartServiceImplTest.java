package org.example.cartservice.service;

import org.example.cartservice.entity.Cart;
import org.example.cartservice.entity.CartItem;
import org.example.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart testCart;
    private CartItem testItem1;
    private CartItem testItem2;

    @BeforeEach
    void setUp() {
        testCart = new Cart();
        testCart.setCartId(1L);
        testCart.setUserId(100L);
        testCart.setItems(new ArrayList<>());
        testCart.setTotalPrice(0.0);

        testItem1 = new CartItem();
        testItem1.setItemId(1L);
        testItem1.setBookId(1L);
        testItem1.setBookTitle("Clean Code");
        testItem1.setPrice(45.99);
        testItem1.setQuantity(2);

        testItem2 = new CartItem();
        testItem2.setItemId(2L);
        testItem2.setBookId(2L);
        testItem2.setBookTitle("Design Patterns");
        testItem2.setPrice(55.99);
        testItem2.setQuantity(1);
    }

    // Tests for getCartByUser
    @Test
    void getCartByUser_shouldReturnExistingCart() {
        testCart.getItems().add(testItem1);
        testCart.setTotalPrice(91.98);

        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));

        Cart result = cartService.getCartByUser(100L);

        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals(1, result.getItems().size());
        assertEquals(91.98, result.getTotalPrice());
        verify(cartRepository, times(1)).findByUserId(100L);
    }

    @Test
    void getCartByUser_shouldCreateNewCartWhenNotExists() {
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setCartId(1L);
            return cart;
        });

        Cart result = cartService.getCartByUser(100L);

        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals(0, result.getItems().size());
        assertEquals(0.0, result.getTotalPrice());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // Tests for addItem
    @Test
    void addItem_shouldAddItemToCart() {
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.addItem(100L, testItem1);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(testItem1.getItemId(), result.getItems().get(0).getItemId());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addItem_shouldCalculateTotalPriceAfterAddingItem() {
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.addItem(100L, testItem1);

        assertNotNull(result);
        double expectedTotal = testItem1.getPrice() * testItem1.getQuantity();
        assertEquals(expectedTotal, result.getTotalPrice(), 0.01);
    }

    @Test
    void addItem_shouldAddMultipleItemsToCart() {
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.addItem(100L, testItem1);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));

        cartService.addItem(100L, testItem2);

        assertEquals(2, testCart.getItems().size());
    }

    // Tests for removeItem
    @Test
    void removeItem_shouldRemoveItemFromCart() {
        testCart.getItems().add(testItem1);
        testCart.getItems().add(testItem2);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.removeItem(100L, 1L);

        assertEquals(1, result.getItems().size());
        assertFalse(result.getItems().stream().anyMatch(item -> item.getItemId().equals(1L)));
        assertTrue(result.getItems().stream().anyMatch(item -> item.getItemId().equals(2L)));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void removeItem_shouldUpdateTotalPriceAfterRemoval() {
        testCart.getItems().add(testItem1);
        testCart.getItems().add(testItem2);
        testCart.setTotalPrice((testItem1.getPrice() * testItem1.getQuantity()) +
                (testItem2.getPrice() * testItem2.getQuantity()));

        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.removeItem(100L, 1L);

        double expectedTotal = testItem2.getPrice() * testItem2.getQuantity();
        assertEquals(expectedTotal, result.getTotalPrice(), 0.01);
    }

    @Test
    void removeItem_shouldNotAffectCartWhenItemNotFound() {
        testCart.getItems().add(testItem1);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.removeItem(100L, 999L);

        assertEquals(1, result.getItems().size());
        assertEquals(testItem1.getItemId(), result.getItems().get(0).getItemId());
    }

    // Tests for updateQuantity
    @Test
    void updateQuantity_shouldUpdateItemQuantity() {
        testCart.getItems().add(testItem1);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.updateQuantity(100L, 1L, 5);

        assertEquals(5, testCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void updateQuantity_shouldUpdateTotalPriceAfterQuantityChange() {
        testCart.getItems().add(testItem1);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.updateQuantity(100L, 1L, 5);

        double expectedTotal = testItem1.getPrice() * 5;
        assertEquals(expectedTotal, result.getTotalPrice(), 0.01);
    }

    @Test
    void updateQuantity_shouldHandleMultipleItems() {
        testCart.getItems().add(testItem1);
        testCart.getItems().add(testItem2);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Cart result = cartService.updateQuantity(100L, 2L, 3);

        assertEquals(2, result.getItems().get(0).getQuantity()); // First item unchanged
        assertEquals(3, result.getItems().get(1).getQuantity()); // Second item updated
    }

    // Tests for clearCart
    @Test
    void clearCart_shouldRemoveAllItemsFromCart() {
        testCart.getItems().add(testItem1);
        testCart.getItems().add(testItem2);
        testCart.setTotalPrice((testItem1.getPrice() * testItem1.getQuantity()) +
                (testItem2.getPrice() * testItem2.getQuantity()));

        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.clearCart(100L);

        assertTrue(testCart.getItems().isEmpty());
        assertEquals(0.0, testCart.getTotalPrice());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void clearCart_shouldResetTotalPrice() {
        testCart.getItems().add(testItem1);
        testCart.setTotalPrice(91.98);

        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.clearCart(100L);

        assertEquals(0.0, testCart.getTotalPrice());
    }

    // Tests for cartTotal
    @Test
    void cartTotal_shouldCalculateCorrectTotalForSingleItem() {
        testCart.getItems().add(testItem1);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));

        double total = cartService.cartTotal(100L);

        double expectedTotal = testItem1.getPrice() * testItem1.getQuantity();
        assertEquals(expectedTotal, total, 0.01);
        verify(cartRepository, times(1)).findByUserId(100L);
    }

    @Test
    void cartTotal_shouldCalculateCorrectTotalForMultipleItems() {
        testCart.getItems().add(testItem1);
        testCart.getItems().add(testItem2);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));

        double total = cartService.cartTotal(100L);

        double expectedTotal = (testItem1.getPrice() * testItem1.getQuantity()) +
                (testItem2.getPrice() * testItem2.getQuantity());
        assertEquals(expectedTotal, total, 0.01);
    }

    @Test
    void cartTotal_shouldReturnZeroForEmptyCart() {
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));

        double total = cartService.cartTotal(100L);

        assertEquals(0.0, total);
    }

    @Test
    void cartTotal_shouldCalculateCorrectlyWithDifferentQuantities() {
        CartItem item1 = new CartItem();
        item1.setItemId(1L);
        item1.setPrice(10.0);
        item1.setQuantity(3);

        CartItem item2 = new CartItem();
        item2.setItemId(2L);
        item2.setPrice(20.0);
        item2.setQuantity(2);

        testCart.getItems().add(item1);
        testCart.getItems().add(item2);
        when(cartRepository.findByUserId(100L)).thenReturn(Optional.of(testCart));

        double total = cartService.cartTotal(100L);

        double expectedTotal = (10.0 * 3) + (20.0 * 2); // 30 + 40 = 70
        assertEquals(expectedTotal, total, 0.01);
    }
}
