package org.example.reviewservice.service.impl;

import org.example.reviewservice.entity.Review;
import org.example.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void addReview_shouldSaveWhenVerifiedAndUnique() {
        Review review = validReview();
        when(reviewRepository.findByBookIdAndUserId(10L, 20L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = reviewService.addReview(review);

        assertEquals(LocalDate.now(), saved.getReviewDate());
        verify(reviewRepository).save(review);
    }

    @Test
    void addReview_shouldThrowWhenUserIsNotVerified() {
        Review review = validReview();
        review.setVerified(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.addReview(review));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void addReview_shouldThrowWhenReviewAlreadyExists() {
        Review review = validReview();
        when(reviewRepository.findByBookIdAndUserId(10L, 20L)).thenReturn(Optional.of(new Review()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.addReview(review));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updateReview_shouldThrowWhenReviewNotFound() {
        Review update = validReview();
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.updateReview(99L, update));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void addReview_shouldThrowWhenRatingIsOutsideAllowedRange() {
        Review review = validReview();
        review.setRating(6);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.addReview(review));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void getAvgRating_shouldReturnZeroWhenRepositoryReturnsNull() {
        when(reviewRepository.avgRatingByBookId(10L)).thenReturn(null);

        Double avg = reviewService.getAvgRating(10L);

        assertEquals(0.0, avg);
    }

    private Review validReview() {
        Review review = new Review();
        review.setBookId(10L);
        review.setUserId(20L);
        review.setRating(4);
        review.setComment("Great read");
        review.setVerified(true);
        return review;
    }
}
