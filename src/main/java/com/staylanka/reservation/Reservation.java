package com.staylanka.reservation;

import com.staylanka.common.BaseEntity;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.promotion.Promotion;
import com.staylanka.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {
    @Column(name = "reservation_reference", nullable = false, unique = true, length = 30)
    private String reservationReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "nightly_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal nightlyPriceSnapshot;

    @Column(name = "gross_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossTotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected Reservation() {
    }

    public Reservation(String reservationReference, CustomerProfile customer, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate, int guestCount,
                       BigDecimal nightlyPriceSnapshot, BigDecimal grossTotal,
                       BigDecimal discountAmount, BigDecimal totalAmount,
                       Promotion promotion, String notes) {
        this.reservationReference = reservationReference;
        this.customer = customer;
        this.status = ReservationStatus.PENDING;
        updateDetails(room, checkInDate, checkOutDate, guestCount, nightlyPriceSnapshot,
                grossTotal, discountAmount, totalAmount, promotion, notes);
    }

    public void updateDetails(Room room, LocalDate checkInDate, LocalDate checkOutDate, int guestCount,
                              BigDecimal nightlyPriceSnapshot, BigDecimal grossTotal,
                              BigDecimal discountAmount, BigDecimal totalAmount,
                              Promotion promotion, String notes) {
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestCount = guestCount;
        this.nightlyPriceSnapshot = nightlyPriceSnapshot;
        this.grossTotal = grossTotal;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.promotion = promotion;
        this.notes = notes;
    }

    public void transitionTo(ReservationStatus status, String reason) {
        this.status = status;
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.REJECTED
                || status == ReservationStatus.NO_SHOW) {
            this.cancellationReason = reason;
        }
    }

    public String getReservationReference() { return reservationReference; }
    public CustomerProfile getCustomer() { return customer; }
    public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public int getGuestCount() { return guestCount; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getNightlyPriceSnapshot() { return nightlyPriceSnapshot; }
    public BigDecimal getGrossTotal() { return grossTotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Promotion getPromotion() { return promotion; }
    public String getCancellationReason() { return cancellationReason; }
    public String getNotes() { return notes; }
    public long getNumberOfNights() { return ChronoUnit.DAYS.between(checkInDate, checkOutDate); }
}

