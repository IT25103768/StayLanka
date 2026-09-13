package com.staylanka.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByStayId(Long stayId);
    Optional<Review> findByStayId(Long stayId);

    @EntityGraph(attributePaths = {"customer", "stay", "stay.room", "stay.room.roomType"})
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "customer.user", "stay", "stay.room", "stay.room.roomType"})
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"stay", "stay.room", "stay.room.roomType"})
    Page<Review> findByCustomerUserEmailIgnoreCase(String email, Pageable pageable);

    long countByStatus(ReviewStatus status);
}

