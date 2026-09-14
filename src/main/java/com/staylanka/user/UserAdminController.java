package com.staylanka.user;

import com.staylanka.common.BusinessRuleException;
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
        prepareCreate(model, new StaffForm(), Role.STAFF);
        return "user/staff-form";
    }

    @PostMapping("/admin/staff")
    public String createStaff(@Valid @ModelAttribute StaffForm staffForm, BindingResult bindingResult,
                              @RequestParam Role role, Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareCreate(model, staffForm, role);
            return "user/staff-form";
        }
        try {
            userAdminService.createStaff(staffForm, role);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("email", "email.exists", ex.getMessage());
            prepareCreate(model, staffForm, role);
            return "user/staff-form";
        } catch (BusinessRuleException ex) {
            bindingResult.reject("staff.invalid", ex.getMessage());
            prepareCreate(model, staffForm, role);
            return "user/staff-form";
        }
        redirectAttributes.addFlashAttribute("success", "Staff account created.");
        return "redirect:/admin/staff";
    }

    @GetMapping("/admin/staff/{id}/edit")
    public String editStaff(@PathVariable Long id, Model model) {
        model.addAttribute("staffUser", userAdminService.staffUser(id));
        model.addAttribute("staffProfile", userAdminService.staffProfile(id));
        model.addAttribute("roles", userAdminService.manageableRoles());
        return "user/staff-edit";
    }

    @PostMapping("/admin/staff/{id}")
    public String updateStaff(@PathVariable Long id,
                              @RequestParam String email,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String jobTitle,
                              @RequestParam Role role,
                              RedirectAttributes redirectAttributes) {
        try {
            userAdminService.updateStaff(id, email, firstName, lastName, jobTitle, role);
            redirectAttributes.addFlashAttribute("success", "Staff account updated.");
        } catch (ConflictException | BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/staff/" + id + "/edit";
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{id}/toggle-active")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userAdminService.toggleStaff(id);
        redirectAttributes.addFlashAttribute("success", "Staff account status updated.");
        return "redirect:/admin/staff";
    }

    private void prepareCreate(Model model, StaffForm form, Role selectedRole) {
        model.addAttribute("staffForm", form);
        model.addAttribute("roles", userAdminService.manageableRoles());
        model.addAttribute("selectedRole", selectedRole);
    }
}
