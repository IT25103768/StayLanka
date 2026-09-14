```java
        package com.staylanka.room;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public class RoomForm {

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private Long roomTypeId;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Nightly price is required")
    @DecimalMin(
            value = "0.01",
            message = "Nightly price must be greater than 0"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "Nightly price must have a maximum of 2 decimal places"
    )
    private BigDecimal nightlyPrice;

    private MultipartFile image;

    public static RoomForm from(Room room) {
        RoomForm form = new RoomForm();

        if (room != null) {
            form.roomNumber = room.getRoomNumber();
            form.roomTypeId = room.getRoomType() != null
                    ? room.getRoomType().getId()
                    : null;
            form.description = room.getDescription();
            form.nightlyPrice = room.getNightlyPrice();
        }

        return form;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber != null
                ? roomNumber.trim()
                : null;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null
                ? description.trim()
                : null;
    }

    public BigDecimal getNightlyPrice() {
        return nightlyPrice;
    }

    public void setNightlyPrice(BigDecimal nightlyPrice) {
        this.nightlyPrice = nightlyPrice;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
```
