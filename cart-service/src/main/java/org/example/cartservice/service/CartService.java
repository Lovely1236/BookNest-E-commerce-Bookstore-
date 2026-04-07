package org.example.cartservice.service;

import org.example.cartservice.entity.Cart;
import org.example.cartservice.entity.CartItem;

public interface CartService {

    Cart getCartByUser(Long userId);

    Cart addItem(Long userId, CartItem item);

    Cart removeItem(Long userId, Long itemId);

    Cart updateQuantity(Long userId, Long itemId, Integer quantity);

    void clearCart(Long userId);
    double cartTotal(Long userId);
}