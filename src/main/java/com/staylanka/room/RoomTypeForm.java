package com.staylanka.room;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RoomTypeForm {

    @NotBlank(message = "Room type name is required")
    @Size(max = 100, message = "Room type name must not exceed 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity must not exceed 20")
    private int capacity = 2;

    @NotBlank(message = "Bed information is required")
    @Size(max = 120, message = "Bed information must not exceed 120 characters")
    private String bedInformation;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Base price must have up to 10 digits and 2 decimal places")
    private BigDecimal basePrice;

    @Size(max = 2000, message = "Amenities must not exceed 2000 characters")
    private String amenities;

    public static RoomTypeForm from(RoomType type) {
        RoomTypeForm form = new RoomTypeForm();

        if (type == null) {
            return form;
        }

        form.name = type.getName();
        form.description = type.getDescription();
        form.capacity = type.getCapacity();
        form.bedInformation = type.getBedInformation();
        form.basePrice = type.getBasePrice();
        form.amenities = type.getAmenities();

        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getBedInformation() {
        return bedInformation;
    }

    public void setBedInformation(String bedInformation) {
        this.bedInformation = bedInformation;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }
}