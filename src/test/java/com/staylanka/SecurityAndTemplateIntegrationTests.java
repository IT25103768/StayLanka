package com.staylanka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void staffCannotAccessAdminOnlyRoutes() throws Exception {
        mockMvc.perform(get("/admin/staff").with(user("staff@test.lk").roles("STAFF")))
                .andExpect(status().isForbidden());
    }
}
