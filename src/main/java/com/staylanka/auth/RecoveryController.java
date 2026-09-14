package com.staylanka.auth;

import com.staylanka.common.BusinessRuleException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RecoveryController {
    private final RecoveryService recovery; private final EntityManager em;
    public RecoveryController(RecoveryService recovery,EntityManager em) {this.recovery=recovery;this.em=em;}
    @GetMapping("/forgot-password") public String forgot(){return "auth/forgot";}
    @PostMapping("/forgot-password") public String request(@RequestParam String email,RedirectAttributes flash){
        recovery.request(email);flash.addFlashAttribute("success","If an active account matches, a recovery request is available to the hotel administrator. Contact the hotel for identity verification and secure token delivery.");return "redirect:/forgot-password";
    }
    @GetMapping("/reset-password") public String reset(){return "auth/reset";}
    @PostMapping("/reset-password") public String reset(@RequestParam String token,@RequestParam String password,@RequestParam String confirmation,Model model,RedirectAttributes flash){
        try{recovery.reset(token,password,confirmation);}catch(BusinessRuleException ex){model.addAttribute("error",ex.getMessage());return "auth/reset";}
        flash.addFlashAttribute("success","Password changed. Sign in with your new password.");return "redirect:/login";
    }
    @GetMapping("/admin/recovery") @Transactional(readOnly=true)
    public String queue(Model model){populate(model);return "auth/recovery-queue";}
    @PostMapping("/admin/recovery")
    public String issue(@RequestParam Long userId,@RequestParam(defaultValue="false") boolean verified,Model model){
        model.addAttribute("issuedToken",recovery.issue(userId,verified));populate(model);return "auth/recovery-queue";
    }
    private void populate(Model model){model.addAttribute("requests",em.createNativeQuery("SELECT u.id,u.email,p.requested_at FROM password_recovery p JOIN app_users u ON u.id=p.user_id WHERE p.used_at IS NULL ORDER BY p.requested_at").setMaxResults(100).getResultList());}
}
