package com.staylanka.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelReservationForm {
    @NotBlank @Size(max = 500)
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

