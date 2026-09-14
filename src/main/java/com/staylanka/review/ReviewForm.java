package com.staylanka.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewForm {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 2000;

    @Min(
            value = MIN_RATING,
            message = "Rating must be at least 1."
    )
    @Max(
            value = MAX_RATING,
            message = "Rating cannot be more than 5."
    )
    private int rating = 5;

    @NotBlank(
            message = "Comment cannot be empty."
    )
    @Size(
            min = MIN_COMMENT_LENGTH,
            max = MAX_COMMENT_LENGTH,
            message = "Comment must be between 10 and 2000 characters."
    )
    private String comment;

    /**
     * Creates a ReviewForm from an existing Review.
     */
    public static ReviewForm from(Review review) {
        if (review == null) {
            throw new IllegalArgumentException(
                    "Review cannot be null."
            );
        }

        ReviewForm form = new ReviewForm();

        form.rating = review.getRating();
        form.comment = review.getComment();

        return form;
    }

    /**
     * Normalizes user input before sending it to the service layer.
     */
    public void normalize() {
        if (comment != null) {
            comment = comment.trim();
        }
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}