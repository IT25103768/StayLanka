package com.staylanka.security;

import com.staylanka.common.NotFoundException;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.customer.CustomerProfileRepository;
import com.staylanka.user.AppUser;
import com.staylanka.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final AppUserRepository userRepository;
    private final CustomerProfileRepository customerRepository;

    public CurrentUserService(AppUserRepository userRepository, CustomerProfileRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public AppUser user(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("Signed-in account was not found.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName()).filter(AppUser::isActive)
                .orElseThrow(() -> new NotFoundException("Signed-in account was not found."));
    }

    public CustomerProfile customer(Authentication authentication) {
        return customerRepository.findByUserId(user(authentication).getId())
                .orElseThrow(() -> new NotFoundException("Customer profile was not found."));
    }
}
