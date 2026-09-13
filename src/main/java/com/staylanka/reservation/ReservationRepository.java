package com.staylanka.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByReservationReference(String reference);

    @EntityGraph(attributePaths = {"customer", "customer.user", "room", "room.roomType", "promotion"})
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"room", "room.roomType", "promotion"})
    Page<Reservation> findByCustomerUserEmailIgnoreCase(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "customer.user", "room", "room.roomType"})
    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "customer.user", "room", "room.roomType"})
    Page<Reservation> findByStatusAndCheckInDateLessThanEqualAndCheckOutDateAfter(
            ReservationStatus status, LocalDate latestCheckIn, LocalDate earliestCheckOut, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "customer.user", "room", "room.roomType", "promotion"})
    @Query("""
            select r from Reservation r
            where (:status is null or r.status = :status)
              and (:fromDate is null or r.checkInDate >= :fromDate)
              and (:toDate is null or r.checkInDate <= :toDate)
              and (lower(r.reservationReference) like lower(concat('%', :term, '%'))
                   or lower(r.customer.user.email) like lower(concat('%', :term, '%'))
                   or lower(r.room.roomNumber) like lower(concat('%', :term, '%')))
            """)
    Page<Reservation> search(@Param("term") String term,
                             @Param("status") ReservationStatus status,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate,
                             Pageable pageable);

    @Query("""
            select count(r) from Reservation r
            where r.room.id = :roomId
              and (:excludeId is null or r.id <> :excludeId)
              and r.status not in (
                com.staylanka.reservation.ReservationStatus.CANCELLED,
                com.staylanka.reservation.ReservationStatus.REJECTED,
                com.staylanka.reservation.ReservationStatus.NO_SHOW)
              and r.checkInDate < :checkOut
              and r.checkOutDate > :checkIn
            """)
    long countBlockingOverlaps(@Param("roomId") Long roomId,
                               @Param("checkIn") LocalDate checkIn,
                               @Param("checkOut") LocalDate checkOut,
                               @Param("excludeId") Long excludeId);

    @Query("""
            select count(r) from Reservation r
            where r.room.id = :roomId
              and r.status in (
                com.staylanka.reservation.ReservationStatus.PENDING,
                com.staylanka.reservation.ReservationStatus.CONFIRMED,
                com.staylanka.reservation.ReservationStatus.CHECKED_IN)
              and (r.status = com.staylanka.reservation.ReservationStatus.CHECKED_IN
                   or r.checkOutDate > :today)
            """)
    long countBlockingOperationalChanges(@Param("roomId") Long roomId,
                                         @Param("today") LocalDate today);

    @Query("""
            select count(r) from Reservation r
            where r.room.roomType.id = :roomTypeId
              and r.status in (
                com.staylanka.reservation.ReservationStatus.PENDING,
                com.staylanka.reservation.ReservationStatus.CONFIRMED,
                com.staylanka.reservation.ReservationStatus.CHECKED_IN)
              and (r.status = com.staylanka.reservation.ReservationStatus.CHECKED_IN
                   or r.checkOutDate > :today)
              and r.guestCount > :capacity
            """)
    long countCapacityConflictsForRoomType(@Param("roomTypeId") Long roomTypeId,
                                           @Param("capacity") int capacity,
                                           @Param("today") LocalDate today);

    long countByStatus(ReservationStatus status);
}
