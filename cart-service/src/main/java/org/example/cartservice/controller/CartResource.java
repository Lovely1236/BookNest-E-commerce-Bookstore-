package org.example.cartservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.cartservice.client.BookClient;
import org.example.cartservice.dto.Book;
import org.example.cartservice.entity.Cart;
import org.example.cartservice.entity.CartItem;
import org.example.cartservice.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartResource {

    private final CartService cartService;

    private BookClient bookClient;


    // GET CART
    @GetMapping("/{userId}")
    public Cart getCart(@PathVariable Long userId) {
        return cartService.getCartByUser(userId);
    }

    @PostMapping("/{userId}/add")
    public Cart addItem(@PathVariable Long userId, @RequestBody CartItem cartItem) {

        // FEIGN CALL
        Book book = bookClient.getBookById(cartItem.getBookId());

        if (book == null) {
            throw new RuntimeException("Book not found");
        }

        cartItem.setBookTitle(book.getTitle());
        cartItem.setPrice(book.getPrice());

        return cartService.addItem(userId, cartItem);
    }
    // REMOVE ITEM
    @DeleteMapping("/{userId}/remove/{itemId}")
    public Cart removeItem(@PathVariable Long userId, @PathVariable Long itemId) {
        return cartService.removeItem(userId, itemId);
    }

    // UPDATE QUANTITY
    @PutMapping("/{userId}/update/{itemId}")
    public Cart updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long itemId,
            @RequestParam int quantity) {

        return cartService.updateQuantity(userId, itemId, quantity);
    }

    // CLEAR CART
    @DeleteMapping("/{userId}/clear")
    public void clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
    }

    // Cart Total
    @GetMapping("/{userId}/total")
    public double getTotal(@PathVariable Long userId) {
        return cartService.cartTotal(userId);
    }
}