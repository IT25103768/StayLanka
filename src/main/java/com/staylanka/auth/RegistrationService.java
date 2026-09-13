package com.staylanka.auth;

import com.staylanka.common.ConflictException;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.customer.CustomerProfileRepository;
import com.staylanka.user.AppUser;
import com.staylanka.user.AppUserRepository;
import com.staylanka.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final AppUserRepository userRepository;
    private final CustomerProfileRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AppUserRepository userRepository,
                               CustomerProfileRepository customerRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already exists for this email address.");
        }
        AppUser user = userRepository.save(new AppUser(email, passwordEncoder.encode(form.getPassword()), Role.CUSTOMER));
        customerRepository.save(new CustomerProfile(user, form.getFirstName().trim(),
                form.getLastName().trim(), trimToNull(form.getPhone())));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

