package org.example.wishlistservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.wishlistservice.entity.Wishlist;
import org.example.wishlistservice.entity.WishlistItem;
import org.example.wishlistservice.service.WishlistService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistResource {

    private final WishlistService wishlistService;

    @GetMapping("/{userId}")
    public Wishlist getWishlist(@PathVariable Long userId) {
        return wishlistService.getWishlistByUser(userId);
    }

    @PostMapping("/add/{userId}")
    public Wishlist addBook(@PathVariable Long userId, @RequestBody WishlistItem item) {
        return wishlistService.addBook(userId, item);
    }

    @DeleteMapping("/remove/{userId}/{itemId}")
    public Wishlist removeBook(@PathVariable Long userId, @PathVariable Long itemId) {
        return wishlistService.removeBook(userId, itemId);
    }

    @DeleteMapping("/clear/{userId}")
    public String clear(@PathVariable Long userId) {
        wishlistService.clearWishlist(userId);
        return "Wishlist Cleared";
    }

    @PostMapping("/move/{userId}/{itemId}")
    public String moveToCart(@PathVariable Long userId, @PathVariable Long itemId) {
        return wishlistService.moveToCart(userId, itemId);
    }

    @GetMapping
    public List<Wishlist> getAll() {
        return wishlistService.getAllWishlists();
    }
}
