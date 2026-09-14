package com.staylanka.room;

import com.staylanka.common.BaseEntity;
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

@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Column(name = "room_number", nullable = false, unique = true, length = 20)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "nightly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal nightlyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected Room() {
    }

    public Room(String roomNumber, RoomType roomType, String description,
                BigDecimal nightlyPrice, RoomStatus status) {

        updateDetails(roomNumber, roomType, description, nightlyPrice);
        this.status = status != null ? status : RoomStatus.AVAILABLE;
    }

    public void updateDetails(String roomNumber, RoomType roomType,
                              String description, BigDecimal nightlyPrice) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Room number cannot be empty");
        }

        if (roomType == null) {
            throw new IllegalArgumentException("Room type cannot be null");
        }

        if (nightlyPrice == null || nightlyPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Nightly price cannot be negative");
        }

        this.roomNumber = roomNumber.trim();
        this.roomType = roomType;
        this.description = description;
        this.nightlyPrice = nightlyPrice;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getNightlyPrice() {
        return nightlyPrice;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Room status cannot be null");
        }

        this.status = status;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}