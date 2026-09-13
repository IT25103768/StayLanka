package com.staylanka.room;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RoomTypeForm {
    @NotBlank @Size(max = 100)
    private String name;
    @Size(max = 2000)
    private String description;
    @Min(1) @Max(20)
    private int capacity = 2;
    @NotBlank @Size(max = 120)
    private String bedInformation;
    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal basePrice;
    @Size(max = 2000)
    private String amenities;

    public static RoomTypeForm from(RoomType type) {
        RoomTypeForm form = new RoomTypeForm();
        form.name = type.getName();
        form.description = type.getDescription();
        form.capacity = type.getCapacity();
        form.bedInformation = type.getBedInformation();
        form.basePrice = type.getBasePrice();
        form.amenities = type.getAmenities();
        return form;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getBedInformation() { return bedInformation; }
    public void setBedInformation(String bedInformation) { this.bedInformation = bedInformation; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
}

