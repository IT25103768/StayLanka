package com.staylanka.stay;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CheckInForm {
    @jakarta.validation.constraints.AssertTrue(message = "Verify the guest identity document before check-in")
    private boolean identityVerified;
    public boolean isIdentityVerified() { return identityVerified; }
    public void setIdentityVerified(boolean value) { identityVerified = value; }
    @NotNull @PastOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime actualCheckIn = LocalDateTime.now().withSecond(0).withNano(0);
    @Min(1)
    private int guestCount = 1;
    @Size(max = 2000)
    private String notes;

    public LocalDateTime getActualCheckIn() { return actualCheckIn; }
    public void setActualCheckIn(LocalDateTime actualCheckIn) { this.actualCheckIn = actualCheckIn; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
