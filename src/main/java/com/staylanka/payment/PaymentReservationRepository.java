package com.staylanka.payment;
import com.staylanka.reservation.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PaymentReservationRepository extends JpaRepository<Reservation,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from Reservation r where r.id=:id")
 Optional<Reservation> lockReservation(@Param("id") Long id);
}
