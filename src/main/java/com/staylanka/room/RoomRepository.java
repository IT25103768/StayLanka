package com.staylanka.room;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByRoomNumberIgnoreCase(String roomNumber);
    boolean existsByRoomNumberIgnoreCaseAndIdNot(String roomNumber, Long id);

    @EntityGraph(attributePaths = "roomType")
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "roomType")
    @Query("""
            select r from Room r
            where r.id = :id
              and r.status in (com.staylanka.room.RoomStatus.AVAILABLE,
                               com.staylanka.room.RoomStatus.OCCUPIED)
              and r.roomType.active = true
            """)
    Optional<Room> findPublicDetailedById(@Param("id") Long id);

    @Query("""
            select r from Room r join fetch r.roomType rt
            where lower(r.roomNumber) like lower(concat('%', :term, '%'))
               or lower(rt.name) like lower(concat('%', :term, '%'))
            """)
    Page<Room> search(@Param("term") String term, Pageable pageable);

    @Query("""
            select r from Room r join fetch r.roomType rt
            where r.status in (com.staylanka.room.RoomStatus.AVAILABLE,
                               com.staylanka.room.RoomStatus.OCCUPIED)
              and rt.active = true
              and (lower(r.roomNumber) like lower(concat('%', :term, '%'))
                   or lower(rt.name) like lower(concat('%', :term, '%')))
            """)
    Page<Room> searchPublic(@Param("term") String term, Pageable pageable);

    @Query("""
            select r from Room r join fetch r.roomType rt
            where r.status in (com.staylanka.room.RoomStatus.AVAILABLE,
                               com.staylanka.room.RoomStatus.OCCUPIED)
              and rt.active = true
              and rt.capacity >= :guests
              and (:typeId is null or rt.id = :typeId)
              and (:maxPrice is null or r.nightlyPrice <= :maxPrice)
              and not exists (
                select reservation.id from Reservation reservation
                where reservation.room = r
                  and reservation.status not in (
                    com.staylanka.reservation.ReservationStatus.CANCELLED,
                    com.staylanka.reservation.ReservationStatus.REJECTED,
                    com.staylanka.reservation.ReservationStatus.NO_SHOW)
                  and reservation.checkInDate < :checkOut
                  and reservation.checkOutDate > :checkIn
              )
            """)
    Page<Room> findAvailable(@Param("checkIn") LocalDate checkIn,
                             @Param("checkOut") LocalDate checkOut,
                             @Param("guests") int guests,
                             @Param("typeId") Long typeId,
                             @Param("maxPrice") BigDecimal maxPrice,
                             Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r join fetch r.roomType where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(RoomStatus status);
}
