package com.staylanka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndTemplateIntegrationTests {

    @Autowired MockMvc mockMvc;

    @Test
    void publicPagesRenderWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your island stay")));
        mockMvc.perform(get("/rooms")).andExpect(status().isOk()).andExpect(view().name("room/search"));
        mockMvc.perform(get("/promotions")).andExpect(status().isOk()).andExpect(view().name("promotion/public-list"));
        mockMvc.perform(get("/reviews")).andExpect(status().isOk()).andExpect(view().name("review/public-list"));
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
    }

    @Test
    void anonymousUserIsRedirectedFromProtectedRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void customerCannotAccessStaffOrAdminRoutes() throws Exception {
        mockMvc.perform(get("/staff/rooms").with(user("customer@test.lk").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/staff").with(user("customer@test.lk").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyStaffCannotModifyModuleOwnedRoutes() throws Exception {
        mockMvc.perform(post("/staff/rooms/999/deactivate")
                        .with(user("legacy-staff@test.lk").roles("STAFF")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void roomManagerCannotModifyOtherModules() throws Exception {
        var manager = user("IT25103762").roles("ROOM_MANAGER");
        mockMvc.perform(post("/staff/reservations/999/confirm").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/requests/999/start").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/promotions/999/toggle-active").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservationManagerCannotModifyOtherModules() throws Exception {
        var manager = user("IT25103763").roles("RESERVATION_MANAGER");
        mockMvc.perform(post("/staff/rooms/999/deactivate").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/requests/999/start").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/customers/999/toggle-active").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileManagerCannotModifyOtherModules() throws Exception {
        var manager = user("IT25103764").roles("PROFILE_MANAGER");
        mockMvc.perform(post("/staff/rooms/999/deactivate").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/reservations/999/confirm").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/requests/999/start").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void stayManagerCannotModifyOtherModules() throws Exception {
        var manager = user("IT25103765").roles("STAY_MANAGER");
        mockMvc.perform(post("/staff/rooms/999/deactivate").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/reservations/999/confirm").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/promotions/999/toggle-active").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void promotionReviewManagerCannotModifyOtherModules() throws Exception {
        var manager = user("IT25103767").roles("PROMOTION_REVIEW_MANAGER");
        mockMvc.perform(post("/staff/rooms/999/deactivate").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/reservations/999/confirm").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/requests/999/start").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void inquiryRequestManagerCannotModifyOtherModules() throws Exception {
        var manager = user("IT25103768").roles("INQUIRY_REQUEST_MANAGER");
        mockMvc.perform(post("/staff/rooms/999/deactivate").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/reservations/999/confirm").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/promotions/999/toggle-active").with(manager).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void moduleManagersCanReadOnlyRequiredCrossModuleData() throws Exception {
        mockMvc.perform(get("/staff/rooms").with(user("IT25103763").roles("RESERVATION_MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/staff/reservations").with(user("IT25103765").roles("STAY_MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/staff/customers").with(user("IT25103768").roles("INQUIRY_REQUEST_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotAccessAdminStaffManagement() throws Exception {
        mockMvc.perform(get("/admin/staff").with(user("staff@test.lk").roles("STAFF")))
                .andExpect(status().isForbidden());
    }
}
