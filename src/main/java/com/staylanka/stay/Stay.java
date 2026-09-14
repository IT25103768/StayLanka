```java
        package com.staylanka.stay;

import com.staylanka.common.BaseEntity;
import com.staylanka.reservation.Reservation;
import com.staylanka.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "stays")
public class Stay extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "actual_check_in", nullable = false)
    private LocalDateTime actualCheckIn;

    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "room_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal roomCharge;

    @Column(
            name = "additional_charge_total",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal additionalChargeTotal = BigDecimal.ZERO;

    @Column(name = "final_total", precision = 12, scale = 2)
    private BigDecimal finalTotal;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    // Required by JPA
    protected Stay() {
    }

    public Stay(
            Reservation reservation,
            Room room,
            LocalDateTime actualCheckIn,
            int guestCount,
            String notes,
            BigDecimal roomCharge
    ) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null");
        }

        if (actualCheckIn == null) {
            throw new IllegalArgumentException("Check-in time cannot be null");
        }

        if (actualCheckIn.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Check-in time cannot be in the future"
            );
        }

        if (guestCount <= 0) {
            throw new IllegalArgumentException(
                    "Guest count must be greater than zero"
            );
        }

        if (roomCharge == null || roomCharge.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Room charge cannot be null or negative"
            );
        }

        this.reservation = reservation;
        this.room = room;
        this.actualCheckIn = actualCheckIn;
        this.guestCount = guestCount;
        this.notes = normalizeNotes(notes);
        this.roomCharge = money(roomCharge);
        this.additionalChargeTotal = BigDecimal.ZERO.setScale(2);
    }

    /**
     * Updates the total amount of additional charges.
     */
    public void updateAdditionalTotal(BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Additional charge total cannot be null or negative"
            );
        }

        if (isCompleted()) {
            throw new IllegalStateException(
                    "Additional charges cannot be changed after checkout"
            );
        }

        this.additionalChargeTotal = money(total);
    }

    /**
     * Completes the stay and calculates the final total.
     */
    public void checkOut(
            LocalDateTime actualCheckOut,
            BigDecimal additionalTotal
    ) {
        if (isCompleted()) {
            throw new IllegalStateException(
                    "Stay has already been checked out"
            );
        }

        if (actualCheckOut == null) {
            throw new IllegalArgumentException(
                    "Check-out time cannot be null"
            );
        }

        if (actualCheckOut.isBefore(actualCheckIn)) {
            throw new IllegalArgumentException(
                    "Check-out time cannot be before check-in time"
            );
        }

        if (actualCheckOut.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Check-out time cannot be in the future"
            );
        }

        if (additionalTotal == null
                || additionalTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Additional charge total cannot be null or negative"
            );
        }

        this.actualCheckOut = actualCheckOut;
        this.additionalChargeTotal = money(additionalTotal);

        this.finalTotal = this.roomCharge
                .add(this.additionalChargeTotal)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns true when the stay has been completed.
     */
    public boolean isCompleted() {
        return actualCheckOut != null;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }

        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDateTime getActualCheckIn() {
        return actualCheckIn;
    }

    public LocalDateTime getActualCheckOut() {
        return actualCheckOut;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getRoomCharge() {
        return roomCharge;
    }

    public BigDecimal getAdditionalChargeTotal() {
        return additionalChargeTotal;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
```
