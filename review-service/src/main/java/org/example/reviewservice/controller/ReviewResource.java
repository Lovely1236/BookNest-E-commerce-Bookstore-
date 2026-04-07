package org.example.reviewservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.reviewservice.entity.Review;
import org.example.reviewservice.service.ReviewService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewResource {

    private final ReviewService reviewService;

    @PostMapping
    public Review addReview(@RequestBody Review review) {
        return reviewService.addReview(review);
    }

    @GetMapping("/book/{bookId}")
    public List<Review> getByBook(@PathVariable Long bookId) {
        return reviewService.getByBook(bookId);
    }

    @GetMapping("/user/{userId}")
    public List<Review> getByUser(@PathVariable Long userId) {
        return reviewService.getByUser(userId);
    }

    @PutMapping("/{id}")
    public Review update(@PathVariable Long id, @RequestBody Review review) {
        return reviewService.updateReview(id, review);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return "Deleted";
    }

    @GetMapping("/avg/{bookId}")
    public Double avgRating(@PathVariable Long bookId) {
        return reviewService.getAvgRating(bookId);
    }

    @GetMapping
    public List<Review> getAll() {
        return reviewService.getAllReviews();
    }
}
