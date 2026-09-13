package com.staylanka.user;

import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {
    private final AppUserRepository userRepository;
    private final StaffProfileRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository userRepository, StaffProfileRepository staffRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createStaff(StaffForm form) {
        if (userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new ConflictException("An account already exists for this email address.");
        }
        AppUser user = userRepository.save(new AppUser(form.getEmail(), passwordEncoder.encode(form.getPassword()), Role.STAFF));
        staffRepository.save(new StaffProfile(user, form.getFirstName().trim(), form.getLastName().trim(),
                form.getJobTitle().trim()));
    }

    @Transactional(readOnly = true)
    public Page<AppUser> staff(String term, int page) {
        return userRepository.findByRoleAndEmailContainingIgnoreCase(Role.STAFF, term == null ? "" : term.trim(),
                PageRequest.of(Math.max(0, page), 12, Sort.by("email").ascending()));
    }

    @Transactional
    public void toggleStaff(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff account was not found."));
        if (user.getRole() != Role.STAFF) {
            throw new NotFoundException("Staff account was not found.");
        }
        user.setActive(!user.isActive());
    }
}

