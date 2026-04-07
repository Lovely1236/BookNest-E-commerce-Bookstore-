package org.example.wishlistservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.wishlistservice.entity.Wishlist;
import org.example.wishlistservice.entity.WishlistItem;
import org.example.wishlistservice.repository.WishlistRepository;
import org.example.wishlistservice.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;

    @Override
    public Wishlist getWishlistByUser(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Wishlist not found for user " + userId));
    }

    @Override
    public Wishlist addBook(Long userId, WishlistItem item) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUserId(userId);
                    newWishlist.setCreatedAt(LocalDate.now());
                    return newWishlist;
                });

        item.setWishlist(wishlist);
        wishlist.getBooks().add(item);
        return wishlistRepository.save(wishlist);
    }

    @Override
    public Wishlist removeBook(Long userId, Long itemId) {
        Wishlist wishlist = getWishlistByUser(userId);
        boolean removed = wishlist.getBooks().removeIf(item -> item.getItemId().equals(itemId));
        if (!removed) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist item not found with id " + itemId);
        }
        return wishlistRepository.save(wishlist);
    }

    @Override
    public void clearWishlist(Long userId) {
        Wishlist wishlist = getWishlistByUser(userId);
        wishlist.getBooks().clear();
        wishlistRepository.save(wishlist);
    }

    @Override
    public String moveToCart(Long userId, Long itemId) {
        getWishlistByUser(userId);
        return "Moved to cart (to be implemented)";
    }

    @Override
    public List<Wishlist> getAllWishlists() {
        return wishlistRepository.findAll();
    }
}
