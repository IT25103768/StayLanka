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
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stay_id", nullable = false, unique = true)
    private Stay stay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    protected Review() {
    }

    public Review(Stay stay, CustomerProfile customer, int rating, String comment) {
        this.stay = stay;
        this.customer = customer;
        this.rating = rating;
        this.comment = comment;
    }

    public void update(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.status = ReviewStatus.PENDING;
    }

    public void moderate(ReviewStatus status) {
        this.status = status;
    }

    public Stay getStay() { return stay; }
    public CustomerProfile getCustomer() { return customer; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public ReviewStatus getStatus() { return status; }
}

