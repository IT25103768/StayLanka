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

    @Column(name = "additional_charge_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal additionalChargeTotal = BigDecimal.ZERO;

    @Column(name = "final_total", precision = 12, scale = 2)
    private BigDecimal finalTotal;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected Stay() {
    }

    public Stay(Reservation reservation, Room room, LocalDateTime actualCheckIn,
                int guestCount, String notes, BigDecimal roomCharge) {
        this.reservation = reservation;
        this.room = room;
        this.actualCheckIn = actualCheckIn;
        this.guestCount = guestCount;
        this.notes = notes;
        this.roomCharge = roomCharge;
    }

    public void updateAdditionalTotal(BigDecimal total) {
        this.additionalChargeTotal = total;
    }

    public void checkOut(LocalDateTime actualCheckOut, BigDecimal additionalTotal) {
        this.actualCheckOut = actualCheckOut;
        this.additionalChargeTotal = additionalTotal;
        this.finalTotal = roomCharge.add(additionalTotal);
    }

    public Reservation getReservation() { return reservation; }
    public Room getRoom() { return room; }
    public LocalDateTime getActualCheckIn() { return actualCheckIn; }
    public LocalDateTime getActualCheckOut() { return actualCheckOut; }
    public int getGuestCount() { return guestCount; }
    public String getNotes() { return notes; }
    public BigDecimal getRoomCharge() { return roomCharge; }
    public BigDecimal getAdditionalChargeTotal() { return additionalChargeTotal; }
    public BigDecimal getFinalTotal() { return finalTotal; }
    public boolean isCompleted() { return actualCheckOut != null; }
}

