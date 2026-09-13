package com.staylanka.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<AppUser> findByRoleAndEmailContainingIgnoreCase(Role role, String email, Pageable pageable);

    List<AppUser> findByRoleAndActiveTrueOrderByEmailAsc(Role role);
}
