package com.staylanka.stay;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StayRepository extends JpaRepository<Stay, Long> {
    boolean existsByReservationId(Long reservationId);
    Optional<Stay> findByReservationId(Long reservationId);

    @EntityGraph(attributePaths = {"reservation", "reservation.customer", "reservation.customer.user",
            "room", "room.roomType"})
    @Query("select s from Stay s where s.id = :id")
    Optional<Stay> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"reservation", "reservation.customer", "reservation.customer.user",
            "room", "room.roomType"})
    @Query("select s from Stay s where s.id = :id")
    Optional<Stay> findDetailedByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"reservation", "room", "room.roomType"})
    Page<Stay> findByReservationCustomerUserEmailIgnoreCase(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"reservation", "reservation.customer", "reservation.customer.user",
            "room", "room.roomType"})
    Page<Stay> findByActualCheckOutIsNullAndVoidedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"reservation", "reservation.customer", "reservation.customer.user",
            "room", "room.roomType"})
    Page<Stay> findByActualCheckOutIsNotNull(Pageable pageable);

    long countByActualCheckOutIsNullAndVoidedFalse();
}
