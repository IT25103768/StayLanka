package com.staylanka;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class NewRoutesIntegrationTests {
    @Autowired MockMvc mvc;
    @Test void recoveryAndAdministrativePagesRender() throws Exception {
        mvc.perform(get("/forgot-password")).andExpect(status().isOk());mvc.perform(get("/reset-password")).andExpect(status().isOk());
        for(String path:new String[]{"/admin/purge","/admin/audit","/admin/recovery","/admin/customers/new","/staff/rooms/maintenance"})mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void purgeRequiresAdminAndCsrf() throws Exception {
        mvc.perform(post("/admin/purge").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        for(String role:new String[]{"CUSTOMER","ROOM_MANAGER","RESERVATION_MANAGER","PROFILE_MANAGER","STAY_MANAGER","PROMOTION_REVIEW_MANAGER","INQUIRY_REQUEST_MANAGER"})mvc.perform(post("/admin/purge").with(user("user").roles(role)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void readonlyCrossModuleUsersCannotOpenEditForms() throws Exception {
        mvc.perform(get("/staff/rooms/1/edit").with(user("manager").roles("RESERVATION_MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/reservations/1/edit").with(user("manager").roles("STAY_MANAGER"))).andExpect(status().isForbidden());
    }
}
