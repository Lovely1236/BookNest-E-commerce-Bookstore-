package org.example.wishlistservice.service;

import org.example.wishlistservice.entity.Wishlist;
import org.example.wishlistservice.entity.WishlistItem;

import java.util.List;

public interface WishlistService {

    Wishlist getWishlistByUser(Long userId);

    Wishlist addBook(Long userId, WishlistItem item);

    Wishlist removeBook(Long userId, Long itemId);

    void clearWishlist(Long userId);

    String moveToCart(Long userId, Long itemId);

    List<Wishlist> getAllWishlists();
}
