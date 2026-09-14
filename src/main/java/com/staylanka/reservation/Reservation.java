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

    @Column(
            name = "reservation_reference",
            nullable = false,
            unique = true,
            length = 30
    )
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

    @Column(
            name = "nightly_price_snapshot",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal nightlyPriceSnapshot;

    @Column(
            name = "gross_total",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal grossTotal;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
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

    /**
     * Required by JPA.
     */
    protected Reservation() {
    }

    /**
     * Creates a new reservation.
     */
    public Reservation(
            String reservationReference,
            CustomerProfile customer,
            Room room,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int guestCount,
            BigDecimal nightlyPriceSnapshot,
            BigDecimal grossTotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            Promotion promotion,
            String notes) {

        validateReservationReference(reservationReference);
        validateCustomer(customer);

        this.reservationReference = reservationReference;
        this.customer = customer;
        this.status = ReservationStatus.PENDING;

        updateDetails(
                room,
                checkInDate,
                checkOutDate,
                guestCount,
                nightlyPriceSnapshot,
                grossTotal,
                discountAmount,
                totalAmount,
                promotion,
                notes
        );
    }

    /**
     * Updates reservation details.
     */
    public void updateDetails(
            Room room,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int guestCount,
            BigDecimal nightlyPriceSnapshot,
            BigDecimal grossTotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            Promotion promotion,
            String notes) {

        validateRoom(room);
        validateDates(checkInDate, checkOutDate);
        validateGuestCount(guestCount);

        validateMoney(nightlyPriceSnapshot, "Nightly price");
        validateMoney(grossTotal, "Gross total");
        validateMoney(discountAmount, "Discount amount");
        validateMoney(totalAmount, "Total amount");

        if (discountAmount.compareTo(grossTotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount amount cannot be greater than gross total"
            );
        }

        BigDecimal calculatedTotal = grossTotal.subtract(discountAmount);

        if (totalAmount.compareTo(calculatedTotal) != 0) {
            throw new IllegalArgumentException(
                    "Total amount must equal gross total minus discount"
            );
        }

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

    /**
     * Changes the reservation status.
     */
    public void transitionTo(
            ReservationStatus newStatus,
            String reason) {

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Reservation status is required"
            );
        }

        validateStatusTransition(newStatus);

        if (requiresReason(newStatus)) {

            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "A reason is required for " + newStatus
                );
            }

            if (reason.length() > 500) {
                throw new IllegalArgumentException(
                        "Reason cannot exceed 500 characters"
                );
            }

            this.cancellationReason = reason.trim();

        } else {
            this.cancellationReason = null;
        }

        this.status = newStatus;
    }

    /**
     * Checks whether the given status requires a reason.
     */
    private boolean requiresReason(ReservationStatus status) {

        return status == ReservationStatus.CANCELLED
                || status == ReservationStatus.REJECTED
                || status == ReservationStatus.NO_SHOW;
    }

    /**
     * Validates allowed reservation status transitions.
     *
     * The exact statuses depend on ReservationStatus enum.
     */
    private void validateStatusTransition(
            ReservationStatus newStatus) {

        if (this.status == null) {
            return;
        }

        if (this.status == newStatus) {
            return;
        }

        switch (this.status) {

            case PENDING -> {
                if (newStatus != ReservationStatus.CONFIRMED
                        && newStatus != ReservationStatus.REJECTED
                        && newStatus != ReservationStatus.CANCELLED) {

                    throw new IllegalStateException(
                            "Cannot change reservation from "
                                    + status + " to " + newStatus
                    );
                }
            }

            case CONFIRMED -> {
                if (newStatus != ReservationStatus.CANCELLED
                        && newStatus != ReservationStatus.NO_SHOW) {

                    throw new IllegalStateException(
                            "Cannot change reservation from "
                                    + status + " to " + newStatus
                    );
                }
            }

            case CANCELLED, REJECTED, NO_SHOW -> {
                throw new IllegalStateException(
                        "Cannot change a " + status + " reservation"
                );
            }

            default -> {
                // Allows additional statuses to be handled
                // by the ReservationStatus enum in the future.
            }
        }
    }

    /**
     * Validates reservation reference.
     */
    private void validateReservationReference(
            String reservationReference) {

        if (reservationReference == null
                || reservationReference.isBlank()) {

            throw new IllegalArgumentException(
                    "Reservation reference is required"
            );
        }

        if (reservationReference.length() > 30) {
            throw new IllegalArgumentException(
                    "Reservation reference cannot exceed 30 characters"
            );
        }
    }

    /**
     * Validates customer.
     */
    private void validateCustomer(CustomerProfile customer) {

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer is required"
            );
        }
    }

    /**
     * Validates room.
     */
    private void validateRoom(Room room) {

        if (room == null) {
            throw new IllegalArgumentException(
                    "Room is required"
            );
        }
    }

    /**
     * Validates check-in and check-out dates.
     */
    private void validateDates(
            LocalDate checkInDate,
            LocalDate checkOutDate) {

        if (checkInDate == null) {
            throw new IllegalArgumentException(
                    "Check-in date is required"
            );
        }

        if (checkOutDate == null) {
            throw new IllegalArgumentException(
                    "Check-out date is required"
            );
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date"
            );
        }
    }

    /**
     * Validates guest count.
     */
    private void validateGuestCount(int guestCount) {

        if (guestCount <= 0) {
            throw new IllegalArgumentException(
                    "Guest count must be greater than zero"
            );
        }
    }

    /**
     * Validates monetary values.
     */
    private void validateMoney(
            BigDecimal amount,
            String fieldName) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative"
            );
        }
    }

    // =========================
    // Getters
    // =========================

    public String getReservationReference() {
        return reservationReference;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getNightlyPriceSnapshot() {
        return nightlyPriceSnapshot;
    }

    public BigDecimal getGrossTotal() {
        return grossTotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getNotes() {
        return notes;
    }

    public long getLockVersion() {
        return lockVersion;
    }

    /**
     * Returns the number of nights between check-in and check-out.
     */
    public long getNumberOfNights() {
        if (checkInDate == null || checkOutDate == null) {
            return 0;
        }

        return ChronoUnit.DAYS.between(
                checkInDate,
                checkOutDate
        );
    }
}
```

