package com.staylanka.promotion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Finds a promotion by its code, ignoring letter case.
     */
    Optional<Promotion> findByCodeIgnoreCase(String code);

    /**
     * Checks whether a promotion code already exists.
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Checks whether a promotion code exists for another promotion.
     * Useful when updating an existing promotion.
     */
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /**
     * Finds active promotions that are valid for the given date range.
     *
     * A promotion is returned when:
     * - It is active
     * - Its start date is on or before the requested end date
     * - Its end date is on or after the requested start date
     */
    Page<Promotion> findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate endDate,
            LocalDate startDate,
            Pageable pageable
    );

    /**
     * Finds active promotions that are valid on a specific date.
     */
    Page<Promotion> findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate date,
            LocalDate dateForEndDate,
            Pageable pageable
    );
}