package org.example.wishlistservice.repository;

import org.example.wishlistservice.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<Wishlist> findByWishlistId(Long wishlistId);

    void deleteByUserId(Long userId);
}
