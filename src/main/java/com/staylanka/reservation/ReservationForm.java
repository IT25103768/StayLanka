package com.staylanka.reservation;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ReservationForm {
    @NotNull
    private Long roomId;
    @NotNull @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;
    @NotNull @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;
    @Min(1)
    private int guestCount = 1;
    @Size(max = 40)
    private String promotionCode;
    @Size(max = 2000)
    private String notes;

    public static ReservationForm from(Reservation reservation) {
        ReservationForm form = new ReservationForm();
        form.roomId = reservation.getRoom().getId();
        form.checkInDate = reservation.getCheckInDate();
        form.checkOutDate = reservation.getCheckOutDate();
        form.guestCount = reservation.getGuestCount();
        form.promotionCode = reservation.getPromotion() == null ? null : reservation.getPromotion().getCode();
        form.notes = reservation.getNotes();
        return form;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

