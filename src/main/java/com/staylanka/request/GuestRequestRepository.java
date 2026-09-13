package com.staylanka.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuestRequestRepository extends JpaRepository<GuestRequest, Long> {
    boolean existsByRequestReference(String reference);

    @EntityGraph(attributePaths = {"customer", "customer.user", "reservation", "assignedStaff"})
    @Query("select r from GuestRequest r where r.id = :id")
    Optional<GuestRequest> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"reservation", "assignedStaff"})
    Page<GuestRequest> findByCustomerUserEmailIgnoreCase(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "customer.user", "reservation", "assignedStaff"})
    @Query("""
            select r from GuestRequest r
            where (:status is null or r.status = :status)
              and (:priority is null or r.priority = :priority)
              and (:type is null or r.type = :type)
              and (lower(r.requestReference) like lower(concat('%', :term, '%'))
                   or lower(r.subject) like lower(concat('%', :term, '%'))
                   or lower(r.customer.user.email) like lower(concat('%', :term, '%')))
            """)
    Page<GuestRequest> search(@Param("term") String term, @Param("status") RequestStatus status,
                              @Param("priority") RequestPriority priority, @Param("type") RequestType type,
                              Pageable pageable);

    @Query("select count(r) from GuestRequest r where r.status not in (com.staylanka.request.RequestStatus.CLOSED, com.staylanka.request.RequestStatus.CANCELLED)")
    long countOpen();
}

