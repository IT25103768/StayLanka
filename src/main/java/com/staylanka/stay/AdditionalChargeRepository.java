package com.staylanka.stay;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AdditionalChargeRepository extends JpaRepository<AdditionalCharge, Long> {
    List<AdditionalCharge> findByStayIdOrderByCreatedAtAsc(Long stayId);

    @Query("select coalesce(sum(c.subtotal), 0) from AdditionalCharge c where c.stay.id = :stayId")
    BigDecimal totalForStay(@Param("stayId") Long stayId);
}

