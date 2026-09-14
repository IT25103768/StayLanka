package com.staylanka.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Page<AppUser> findByRoleAndEmailContainingIgnoreCase(Role role, String email, Pageable pageable);

    Page<AppUser> findByRoleInAndEmailContainingIgnoreCase(Collection<Role> roles, String email, Pageable pageable);

    List<AppUser> findByRoleAndActiveTrueOrderByEmailAsc(Role role);

    List<AppUser> findByRoleInAndActiveTrueOrderByEmailAsc(Collection<Role> roles);
}
