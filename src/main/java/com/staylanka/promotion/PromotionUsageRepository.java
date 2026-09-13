package com.staylanka.promotion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    Optional<PromotionUsage> findByReservationId(Long reservationId);
    void deleteByReservationId(Long reservationId);
}
