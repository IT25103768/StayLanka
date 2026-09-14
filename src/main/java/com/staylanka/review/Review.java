package com.staylanka.review;

import com.staylanka.common.BaseEntity;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.stay.Stay;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 2000;

    /**
     * A stay can have at most one review.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stay_id",
            nullable = false,
            unique = true
    )
    private Stay stay;

    /**
     * A customer can write multiple reviews for different stays.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private CustomerProfile customer;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = MAX_COMMENT_LENGTH)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ReviewStatus status = ReviewStatus.PENDING;

    /**
     * Required by JPA.
     */
    protected Review() {
    }

    /**
     * Creates a new review.
     */
    public Review(
            Stay stay,
            CustomerProfile customer,
            int rating,
            String comment
    ) {
        validateStay(stay);
        validateCustomer(customer);
        validateRating(rating);
        String normalizedComment = normalizeComment(comment);

        this.stay = stay;
        this.customer = customer;
        this.rating = rating;
        this.comment = normalizedComment;
        this.status = ReviewStatus.PENDING;
    }

    /**
     * Updates the review content.
     *
     * When a review is edited, it must be moderated again.
     */
    public void update(
            int rating,
            String comment
    ) {
        validateRating(rating);

        String normalizedComment = normalizeComment(comment);

        this.rating = rating;
        this.comment = normalizedComment;
        this.status = ReviewStatus.PENDING;
    }

    /**
     * Changes the moderation status of the review.
     */
    public void moderate(ReviewStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Review status cannot be null."
            );
        }

        this.status = status;
    }

    /**
     * Approves the review.
     */
    public void approve() {
        this.status = ReviewStatus.APPROVED;
    }

    /**
     * Rejects the review.
     */
    public void reject() {
        this.status = ReviewStatus.REJECTED;
    }

    /**
     * Resets the review to pending moderation.
     */
    public void markAsPending() {
        this.status = ReviewStatus.PENDING;
    }

    private void validateStay(Stay stay) {
        if (stay == null) {
            throw new IllegalArgumentException(
                    "Stay cannot be null."
            );
        }
    }

    private void validateCustomer(CustomerProfile customer) {
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer cannot be null."
            );
        }
    }

    private void validateRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    "Rating must be between "
                            + MIN_RATING
                            + " and "
                            + MAX_RATING
                            + "."
            );
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException(
                    "Review comment cannot be empty."
            );
        }

        String normalizedComment = comment.trim();

        if (normalizedComment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Review comment cannot exceed "
                            + MAX_COMMENT_LENGTH
                            + " characters."
            );
        }

        return normalizedComment;
    }

    public Stay getStay() {
        return stay;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public ReviewStatus getStatus() {
        return status;
    }
}