package org.example.cartservice.service;
import lombok.RequiredArgsConstructor;
import org.example.cartservice.entity.Cart;
import org.example.cartservice.entity.CartItem;
import org.example.cartservice.repository.CartRepository;
import org.example.cartservice.service.CartService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    public Cart getCartByUser(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    cart.setItems(new ArrayList<>());
                    cart.setTotalPrice(0.0);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Cart addItem(Long userId, CartItem item) {
        Cart cart = getCartByUser(userId);
        cart.getItems().add(item);
        updateTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(Long userId, Long itemId) {
        Cart cart = getCartByUser(userId);
        cart.getItems().removeIf(i -> i.getItemId().equals(itemId));
        updateTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart updateQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = getCartByUser(userId);

        for (CartItem item : cart.getItems()) {
            if (item.getItemId().equals(itemId)) {
                item.setQuantity(quantity);
            }
        }

        updateTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getCartByUser(userId);
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);
    }

    private void updateTotal(Cart cart) {
        double total = cart.getItems()
                .stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        cart.setTotalPrice(total);
    }
    @Override
    public double cartTotal(Long userId) {
        Cart cart = getCartByUser(userId);

        return cart.getItems()
                .stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
    }
}
