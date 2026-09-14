package com.staylanka.user;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class UserAdminService {
    private static final Set<Role> MANAGEABLE_ROLES = EnumSet.of(
            Role.ROOM_MANAGER, Role.RESERVATION_MANAGER, Role.PROFILE_MANAGER,
            Role.STAY_MANAGER, Role.PROMOTION_REVIEW_MANAGER, Role.INQUIRY_REQUEST_MANAGER, Role.STAFF);

    private final AppUserRepository userRepository;
    private final StaffProfileRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository userRepository, StaffProfileRepository staffRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Role> manageableRoles() {
        return List.copyOf(MANAGEABLE_ROLES);
    }

    @Transactional
    public void createStaff(StaffForm form, Role role) {
        validateRole(role);
        if (userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new ConflictException("An account already exists for this email address.");
        }
        AppUser user = userRepository.save(new AppUser(form.getEmail(), passwordEncoder.encode(form.getPassword()), role));
        staffRepository.save(new StaffProfile(user, form.getFirstName().trim(), form.getLastName().trim(),
                form.getJobTitle().trim()));
    }

    @Transactional(readOnly = true)
    public Page<AppUser> staff(String term, int page) {
        return userRepository.findByRoleInAndEmailContainingIgnoreCase(MANAGEABLE_ROLES,
                term == null ? "" : term.trim(),
                PageRequest.of(Math.max(0, page), 12, Sort.by("email").ascending()));
    }

    @Transactional(readOnly = true)
    public AppUser staffUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff account was not found."));
        if (!MANAGEABLE_ROLES.contains(user.getRole())) {
            throw new NotFoundException("Staff account was not found.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public StaffProfile staffProfile(Long id) {
        AppUser user = staffUser(id);
        return staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Staff profile was not found."));
    }

    @Transactional
    public void updateStaff(Long id, String email, String firstName, String lastName, String jobTitle, Role role) {
        validateRole(role);
        validateText(email, "Email", 190);
        validateText(firstName, "First name", 80);
        validateText(lastName, "Last name", 80);
        validateText(jobTitle, "Job title", 100);
        AppUser user = staffUser(id);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("An account already exists for this email address.");
        }
        StaffProfile profile = staffRepository.findByUserId(id)
                .orElseThrow(() -> new NotFoundException("Staff profile was not found."));
        user.setEmail(email);
        user.setRole(role);
        profile.update(firstName.trim(), lastName.trim(), jobTitle.trim());
    }

    @Transactional
    public void toggleStaff(Long id) {
        AppUser user = staffUser(id);
        user.setActive(!user.isActive());
    }

    private void validateRole(Role role) {
        if (role == null || !MANAGEABLE_ROLES.contains(role)) {
            throw new BusinessRuleException("Choose a valid operational staff role.");
        }
    }

    private void validateText(String value, String label, int max) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(label + " is required.");
        }
        if (value.trim().length() > max) {
            throw new BusinessRuleException(label + " is too long.");
        }
    }
}
