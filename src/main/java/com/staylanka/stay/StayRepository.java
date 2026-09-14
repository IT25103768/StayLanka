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

    String DETAILED_GRAPH = "reservation.customer.user,room.roomType";
    String BASIC_GRAPH = "reservation,room.roomType";

    boolean existsByReservationId(Long reservationId);

    Optional<Stay> findByReservationId(Long reservationId);

    /**
     * Finds a stay with all related customer and room information.
     */
    @EntityGraph(attributePaths = {
            "reservation",
            "reservation.customer",
            "reservation.customer.user",
            "room",
            "room.roomType"
    })
    @Query("SELECT s FROM Stay s WHERE s.id = :id")
    Optional<Stay> findDetailedById(@Param("id") Long id);

    /**
     * Finds and locks a stay for update.
     * Used for operations such as check-out where concurrent
     * modifications must be prevented.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "reservation",
            "reservation.customer",
            "reservation.customer.user",
            "room",
            "room.roomType"
    })
    @Query("SELECT s FROM Stay s WHERE s.id = :id")
    Optional<Stay> findDetailedByIdForUpdate(@Param("id") Long id);

    /**
     * Finds a customer's stay history.
     */
    @EntityGraph(attributePaths = {
            "reservation",
            "room",
            "room.roomType"
    })
    Page<Stay> findByReservationCustomerUserEmailIgnoreCase(
            String email,
            Pageable pageable
    );

    /**
     * Finds currently active stays.
     */
    @EntityGraph(attributePaths = {
            "reservation",
            "reservation.customer",
            "reservation.customer.user",
            "room",
            "room.roomType"
    })
    Page<Stay> findByActualCheckOutIsNull(Pageable pageable);

    /**
     * Finds completed stays.
     */
    @EntityGraph(attributePaths = {
            "reservation",
            "reservation.customer",
            "reservation.customer.user",
            "room",
            "room.roomType"
    })
    Page<Stay> findByActualCheckOutIsNotNull(Pageable pageable);

    /**
     * Counts all currently active stays.
     */
    long countByActualCheckOutIsNull();
}