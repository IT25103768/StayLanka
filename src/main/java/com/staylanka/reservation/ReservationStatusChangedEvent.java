package com.staylanka.reservation;

public record ReservationStatusChangedEvent(
        Long reservationId,
        String reference,
        ReservationStatus oldStatus,
        ReservationStatus newStatus
) {
}