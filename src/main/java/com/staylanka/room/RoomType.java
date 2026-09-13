package com.staylanka.room;

import com.staylanka.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "room_types")
public class RoomType extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "bed_information", nullable = false, length = 120)
    private String bedInformation;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(nullable = false)
    private boolean active = true;

    protected RoomType() {
    }

    public RoomType(String name, String description, int capacity, String bedInformation,
                    BigDecimal basePrice, String amenities) {
        update(name, description, capacity, bedInformation, basePrice, amenities);
    }

    public void update(String name, String description, int capacity, String bedInformation,
                       BigDecimal basePrice, String amenities) {
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.bedInformation = bedInformation;
        this.basePrice = basePrice;
        this.amenities = amenities;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCapacity() { return capacity; }
    public String getBedInformation() { return bedInformation; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getAmenities() { return amenities; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

