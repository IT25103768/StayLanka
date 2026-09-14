package com.staylanka;

import com.staylanka.auth.RegistrationForm;
import com.staylanka.auth.RegistrationService;
import com.staylanka.request.GuestRequest;
import com.staylanka.request.GuestRequestForm;
import com.staylanka.request.GuestRequestService;
import com.staylanka.request.RequestPriority;
import com.staylanka.request.RequestType;
import com.staylanka.user.AppUser;
import com.staylanka.user.AppUserRepository;
import com.staylanka.user.Role;
import com.staylanka.user.StaffForm;
import com.staylanka.user.StaffProfileRepository;
import com.staylanka.user.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleAwareCrudIntegrationTests {

    @Autowired UserAdminService userAdminService;
    @Autowired AppUserRepository userRepository;
    @Autowired StaffProfileRepository staffProfileRepository;
    @Autowired RegistrationService registrationService;
    @Autowired GuestRequestService requestService;

    @Test
    void adminStaffCrudSupportsModuleRoles() {
        StaffForm form = new StaffForm();
        form.setEmail("room.owner@staylanka.test");
        form.setPassword("StrongPass123");
        form.setFirstName("Room");
        form.setLastName("Owner");
        form.setJobTitle("Room Manager");

        userAdminService.createStaff(form, Role.ROOM_MANAGER);
        AppUser user = userRepository.findByEmailIgnoreCase(form.getEmail()).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.ROOM_MANAGER);
        assertThat(userAdminService.staff("room.owner", 0).getContent()).contains(user);

        userAdminService.updateStaff(user.getId(), "room.lead@staylanka.test", "Room", "Lead",
                "Senior Room Manager", Role.ROOM_MANAGER);
        assertThat(user.getEmail()).isEqualTo("room.lead@staylanka.test");
        assertThat(staffProfileRepository.findByUserId(user.getId()).orElseThrow().getJobTitle())
                .isEqualTo("Senior Room Manager");

        userAdminService.toggleStaff(user.getId());
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void inquiryManagerCanBeAssignedToGuestRequest() {
        Authentication customer = registerCustomer("request.owner.customer@staylanka.test");

        StaffForm staffForm = new StaffForm();
        staffForm.setEmail("request.owner@staylanka.test");
        staffForm.setPassword("StrongPass123");
        staffForm.setFirstName("Request");
        staffForm.setLastName("Owner");
        staffForm.setJobTitle("Inquiry Manager");
        userAdminService.createStaff(staffForm, Role.INQUIRY_REQUEST_MANAGER);
        AppUser manager = userRepository.findByEmailIgnoreCase(staffForm.getEmail()).orElseThrow();
        Authentication managerAuth = authentication(manager.getEmail(), Role.INQUIRY_REQUEST_MANAGER);

        GuestRequestForm requestForm = new GuestRequestForm();
        requestForm.setCategory("Arrival");
        requestForm.setSubject("Airport pickup");
        requestForm.setDescription("Please arrange an airport pickup.");
        requestForm.setType(RequestType.SPECIAL_REQUEST);
        requestForm.setPriority(RequestPriority.NORMAL);
        GuestRequest request = requestService.create(customer, requestForm);

        assertThat(requestService.activeStaff()).contains(manager);
        requestService.assign(managerAuth, request.getId(), manager.getId());
        assertThat(request.getAssignedStaff()).isEqualTo(manager);
    }

    private Authentication registerCustomer(String email) {
        RegistrationForm form = new RegistrationForm();
        form.setEmail(email);
        form.setPassword("StrongPass123");
        form.setConfirmPassword("StrongPass123");
        form.setFirstName("Test");
        form.setLastName("Customer");
        form.setPhone("+94771234567");
        registrationService.register(form);
        return authentication(email, Role.CUSTOMER);
    }

    private Authentication authentication(String email, Role role) {
        return new UsernamePasswordAuthenticationToken(email, "",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
