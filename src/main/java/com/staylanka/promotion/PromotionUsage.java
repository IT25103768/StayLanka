package com.staylanka.promotion;

import com.staylanka.common.BaseEntity;
import com.staylanka.reservation.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion_usages")
public class PromotionUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    /**
     * A reservation can have at most one promotion usage.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reservation_id",
            nullable = false,
            unique = true
    )
    private Reservation reservation;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;

    /**
     * Required by JPA.
     */
    protected PromotionUsage() {
    }

    public PromotionUsage(
            Promotion promotion,
            Reservation reservation,
            BigDecimal discountAmount
    ) {
        validatePromotion(promotion);
        validateReservation(reservation);
        validateDiscountAmount(discountAmount);

        this.promotion = promotion;
        this.reservation = reservation;
        this.discountAmount = discountAmount;
    }

    /**
     * Updates the promotion and discount amount.
     *
     * The reservation is intentionally not changed because
     * a promotion usage belongs to a specific reservation.
     */
    public void update(
            Promotion promotion,
            BigDecimal discountAmount
    ) {
        validatePromotion(promotion);
        validateDiscountAmount(discountAmount);

        this.promotion = promotion;
        this.discountAmount = discountAmount;
    }

    private void validatePromotion(Promotion promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException(
                    "Promotion cannot be null."
            );
        }
    }

    private void validateReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "Reservation cannot be null."
            );
        }
    }

    private void validateDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null) {
            throw new IllegalArgumentException(
                    "Discount amount cannot be null."
            );
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Discount amount cannot be negative."
            );
        }
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
}