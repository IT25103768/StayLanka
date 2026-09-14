package com.staylanka.common;

import com.staylanka.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

@Controller
public class ControlController {
    private final PurgeService purge;
    private final EntityManager em;
    private final CurrentUserService current;
    public ControlController(PurgeService purge, EntityManager em, CurrentUserService current) {
        this.purge=purge; this.em=em; this.current=current;
    }
    @GetMapping("/admin/purge")
    public String purgePage(@RequestParam(defaultValue="Room") String type, Model model) {
        model.addAttribute("types", PurgeService.TYPES.keySet().stream().sorted().toList());
        model.addAttribute("type",type); model.addAttribute("rows",purge.rows(type)); return "control/purge";
    }
    @PostMapping("/admin/purge")
    public String purge(@RequestParam String type, @RequestParam Long id, @RequestParam String confirmation, RedirectAttributes flash) {
        purge.delete(type,id,confirmation); flash.addFlashAttribute("success","Record permanently deleted from the database.");
        return "redirect:/admin/purge?type="+type;
    }
    @GetMapping("/admin/audit")
    @Transactional(readOnly=true)
    public String audit(@RequestParam(defaultValue="0") int page, Model model) {
        model.addAttribute("page",Math.max(0,page));
        model.addAttribute("events", em.createNativeQuery("SELECT occurred_at,actor,entity_type,entity_id,action,summary FROM audit_events ORDER BY id DESC")
                .setFirstResult(Math.max(0,page)*50).setMaxResults(50).getResultList()); return "control/audit";
    }
    @GetMapping("/notifications")
    @Transactional(readOnly=true)
    public String notifications(Authentication auth, Model model) {
        model.addAttribute("items", em.createNativeQuery("SELECT id,message,link,created_at,read_at FROM notifications WHERE recipient_id=?1 ORDER BY id DESC")
                .setParameter(1,current.user(auth).getId()).setMaxResults(100).getResultList()); return "control/notifications";
    }
    @PostMapping("/notifications/{id}/read")
    @Transactional
    public String read(Authentication auth,@PathVariable Long id) {
        int count=em.createNativeQuery("UPDATE notifications SET read_at=?1 WHERE id=?2 AND recipient_id=?3")
                .setParameter(1,LocalDateTime.now()).setParameter(2,id).setParameter(3,current.user(auth).getId()).executeUpdate();
        if(count==0) throw new NotFoundException("Notification not found."); return "redirect:/notifications";
    }
}
