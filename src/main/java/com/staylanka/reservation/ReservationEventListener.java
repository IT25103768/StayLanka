package com.staylanka.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationEventListener.class);

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onStatusChanged(ReservationStatusChangedEvent event) {

        if (event == null) {
            log.warn("Received null reservation status change event.");
            return;
        }

        log.info(
                "Reservation status changed: reference={}, oldStatus={}, newStatus={}",
                event.reference(),
                event.oldStatus(),
                event.newStatus()
        );
    }
}