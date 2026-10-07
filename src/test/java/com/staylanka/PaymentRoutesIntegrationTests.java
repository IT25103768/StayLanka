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
class PaymentRoutesIntegrationTests {
 @Autowired MockMvc mvc;
 @Test void unauthenticatedVisitorMustLogin() throws Exception {
  mvc.perform(get("/payments")).andExpect(status().is3xxRedirection());
 }
 @Test void unrelatedRolesCannotListAccounts() throws Exception {
  for(String role:new String[]{"CUSTOMER","ROOM_MANAGER","PROFILE_MANAGER","PROMOTION_REVIEW_MANAGER","STAFF"})
   mvc.perform(get("/payments").with(user("user").roles(role))).andExpect(status().isForbidden());
 }
 @Test void staffPaymentListRenders() throws Exception {
  mvc.perform(get("/payments").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
 }
 @Test void paymentRecordingRequiresCsrf() throws Exception {
  mvc.perform(post("/payments/reservations/1").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
 }
}
