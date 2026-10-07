package com.staylanka.payment;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PaymentRepository extends JpaRepository<PaymentEntry,Long> {
 List<PaymentEntry> findByReservationIdOrderByCreatedAtDescIdDesc(Long reservationId);
 Optional<PaymentEntry> findByRequestKey(String key);
 @EntityGraph(attributePaths={"reservation","reservation.customer","reservation.customer.user","reservation.room","reservation.room.roomType"})
 Optional<PaymentEntry> findByReference(String reference);
}
