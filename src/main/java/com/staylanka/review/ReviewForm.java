package com.staylanka.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewForm {
    @Min(1) @Max(5)
    private int rating = 5;
    @NotBlank @Size(min = 10, max = 2000)
    private String comment;

    public static ReviewForm from(Review review) {
        ReviewForm form = new ReviewForm();
        form.rating = review.getRating();
        form.comment = review.getComment();
        return form;
    }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}

