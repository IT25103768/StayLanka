package com.staylanka.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Checks whether a review already exists for a stay.
     */
    boolean existsByStayId(Long stayId);

    /**
     * Finds the review associated with a specific stay.
     */
    Optional<Review> findByStayId(Long stayId);

    /**
     * Finds reviews by moderation status.
     *
     * Fetches the customer and stay information required
     * for displaying review information.
     */
    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "stay",
            "stay.room",
            "stay.room.roomType"
    })
    Page<Review> findByStatus(
            ReviewStatus status,
            Pageable pageable
    );

    /**
     * Finds a review with all required related entities.
     *
     * Used when viewing, updating, deleting or moderating
     * a specific review.
     */
    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "stay",
            "stay.room",
            "stay.room.roomType"
    })
    @Query("""
            SELECT r
            FROM Review r
            WHERE r.id = :id
            """)
    Optional<Review> findDetailedById(
            @Param("id") Long id
    );

    /**
     * Finds reviews belonging to a specific customer.
     */
    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "stay",
            "stay.room",
            "stay.room.roomType"
    })
    Page<Review> findByCustomerUserEmailIgnoreCase(
            String email,
            Pageable pageable
    );

    /**
     * Counts reviews waiting for moderation.
     */
    long countByStatus(ReviewStatus status);
}