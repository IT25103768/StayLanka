package com.staylanka.user;

import com.staylanka.common.ConflictException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserAdminController {
    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/admin/staff")
    public String staff(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("staffPage", userAdminService.staff(q, page));
        model.addAttribute("q", q);
        return "user/staff-list";
    }

    @GetMapping("/admin/staff/new")
    public String newStaff(Model model) {
        model.addAttribute("staffForm", new StaffForm());
        return "user/staff-form";
    }

    @PostMapping("/admin/staff")
    public String createStaff(@Valid @ModelAttribute StaffForm staffForm, BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "user/staff-form";
        }
        try {
            userAdminService.createStaff(staffForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("email", "email.exists", ex.getMessage());
            return "user/staff-form";
        }
        redirectAttributes.addFlashAttribute("success", "Staff account created.");
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{id}/toggle-active")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userAdminService.toggleStaff(id);
        redirectAttributes.addFlashAttribute("success", "Staff account status updated.");
        return "redirect:/admin/staff";
    }
}

