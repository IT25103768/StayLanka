package com.staylanka.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    Optional<CustomerProfile> findById(Long id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    Optional<CustomerProfile> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    Optional<CustomerProfile> findByUserEmailIgnoreCase(String email);

    @Query("""
            select c from CustomerProfile c
            join fetch c.user u
            where lower(concat(c.firstName, ' ', c.lastName)) like lower(concat('%', :term, '%'))
               or lower(u.email) like lower(concat('%', :term, '%'))
               or lower(coalesce(c.phone, '')) like lower(concat('%', :term, '%'))
            """)
    Page<CustomerProfile> search(@Param("term") String term, Pageable pageable);
}
