package com.staylanka.room;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public class RoomForm {
    @NotBlank @Size(max = 20)
    private String roomNumber;
    @NotNull
    private Long roomTypeId;
    @Size(max = 2000)
    private String description;
    @NotNull @DecimalMin("0.01")
    private BigDecimal nightlyPrice;
    @NotNull
    private RoomStatus status = RoomStatus.AVAILABLE;
    private MultipartFile image;

    public static RoomForm from(Room room) {
        RoomForm form = new RoomForm();
        form.roomNumber = room.getRoomNumber();
        form.roomTypeId = room.getRoomType().getId();
        form.description = room.getDescription();
        form.nightlyPrice = room.getNightlyPrice();
        form.status = room.getStatus();
        return form;
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Long getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Long roomTypeId) { this.roomTypeId = roomTypeId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getNightlyPrice() { return nightlyPrice; }
    public void setNightlyPrice(BigDecimal nightlyPrice) { this.nightlyPrice = nightlyPrice; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}

