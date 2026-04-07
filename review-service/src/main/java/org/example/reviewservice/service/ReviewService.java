package org.example.reviewservice.service;

import org.example.reviewservice.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    Review addReview(Review review);

    List<Review> getByBook(Long bookId);

    List<Review> getByUser(Long userId);

    Review updateReview(Long reviewId, Review review);

    void deleteReview(Long reviewId);

    Double getAvgRating(Long bookId);

    List<Review> getAllReviews();

    Optional<Review> getReviewById(Long reviewId);
}
