package org.example.reviewservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.reviewservice.entity.Review;
import org.example.reviewservice.repository.ReviewRepository;
import org.example.reviewservice.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public Review addReview(Review review) {
        validateReview(review);
        if (!review.isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only verified users can submit reviews");
        }
        reviewRepository.findByBookIdAndUserId(review.getBookId(), review.getUserId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Review already exists for this user and book"
                    );
                });
        review.setReviewDate(LocalDate.now());
        return reviewRepository.save(review);
    }

    @Override
    public List<Review> getByBook(Long bookId) {
        return reviewRepository.findByBookId(bookId);
    }

    @Override
    public List<Review> getByUser(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    @Override
    public Review updateReview(Long reviewId, Review updated) {
        validateReview(updated);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found with id " + reviewId));

        review.setRating(updated.getRating());
        review.setComment(updated.getComment());
        review.setVerified(updated.isVerified());

        return reviewRepository.save(review);
    }

    @Override
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found with id " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }

    @Override
    public Double getAvgRating(Long bookId) {
        Double avg = reviewRepository.avgRatingByBookId(bookId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Override
    public Optional<Review> getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    private void validateReview(Review review) {
        if (review.getBookId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookId is required");
        }
        if (review.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be between 1 and 5");
        }
    }
}
