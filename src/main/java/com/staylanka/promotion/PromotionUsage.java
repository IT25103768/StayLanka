package com.staylanka.promotion;

import com.staylanka.common.BaseEntity;
import com.staylanka.reservation.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion_usages")
public class PromotionUsage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    protected PromotionUsage() {
    }

    public PromotionUsage(Promotion promotion, Reservation reservation, BigDecimal discountAmount) {
        this.promotion = promotion;
        this.reservation = reservation;
        this.discountAmount = discountAmount;
    }

    public void update(Promotion promotion, BigDecimal discountAmount) {
        this.promotion = promotion;
        this.discountAmount = discountAmount;
    }

    public Promotion getPromotion() { return promotion; }
    public Reservation getReservation() { return reservation; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
}
