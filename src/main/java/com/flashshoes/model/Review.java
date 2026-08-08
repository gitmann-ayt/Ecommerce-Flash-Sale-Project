package com.flashshoes.model;

import java.time.LocalDate;

/**
 * A single customer review attached to one Product.
 * Reviews are seeded (fake) data for demo purposes, plus any a customer adds live.
 */
public class Review {

    private final String reviewId;
    private final String productId;
    private final String reviewerName;
    private final int rating; // 1-5
    private final String comment;
    private final LocalDate date;

    public Review(String reviewId, String productId, String reviewerName,
                   int rating, String comment, LocalDate date) {
        this.reviewId = reviewId;
        this.productId = productId;
        this.reviewerName = reviewerName;
        this.rating = Math.max(1, Math.min(5, rating));
        this.comment = comment;
        this.date = date;
    }

    public String getReviewId() { return reviewId; }
    public String getProductId() { return productId; }
    public String getReviewerName() { return reviewerName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDate getDate() { return date; }

    public String getStars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }
}
