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
import java.math.RoundingMode;

@Entity
@Table(name = "promotion_usages")
public class PromotionUsage extends BaseEntity {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "promotion_id",
            nullable = false
    )
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
            scale = MONEY_SCALE
    )
    private BigDecimal discountAmount;

    /**
     * Required by JPA.
     */
    protected PromotionUsage() {
    }

    /**
     * Creates a promotion usage for a reservation.
     */
    public PromotionUsage(
            Promotion promotion,
            Reservation reservation,
            BigDecimal discountAmount
    ) {
        this.promotion = requirePromotion(promotion);
        this.reservation = requireReservation(reservation);
        this.discountAmount = normalizeDiscountAmount(discountAmount);
    }

    /**
     * Updates the promotion and discount amount.
     *
     * The reservation is intentionally immutable because
     * this usage belongs to a specific reservation.
     */
    public void update(
            Promotion promotion,
            BigDecimal discountAmount
    ) {
        this.promotion = requirePromotion(promotion);
        this.discountAmount = normalizeDiscountAmount(discountAmount);
    }

    /**
     * Updates only the discount amount.
     */
    public void updateDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = normalizeDiscountAmount(discountAmount);
    }

    /**
     * Validates and returns a promotion.
     */
    private Promotion requirePromotion(Promotion promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException(
                    "Promotion cannot be null."
            );
        }

        return promotion;
    }

    /**
     * Validates and returns a reservation.
     */
    private Reservation requireReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "Reservation cannot be null."
            );
        }

        return reservation;
    }

    /**
     * Validates and normalizes the discount amount.
     */
    private BigDecimal normalizeDiscountAmount(
            BigDecimal discountAmount
    ) {
        if (discountAmount == null) {
            throw new IllegalArgumentException(
                    "Discount amount cannot be null."
            );
        }

        if (discountAmount.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Discount amount cannot be negative."
            );
        }

        if (discountAmount.scale() > MONEY_SCALE) {
            throw new IllegalArgumentException(
                    "Discount amount cannot have more than 2 decimal places."
            );
        }

        return discountAmount.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
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