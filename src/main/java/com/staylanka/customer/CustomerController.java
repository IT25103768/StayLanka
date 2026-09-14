package com.staylanka.customer;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
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
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // Display the logged-in customer's profile
    @GetMapping("/customer/profile")
    public String profile(Authentication authentication, Model model) {
        CustomerProfile profile = customerService.current(authentication);

        model.addAttribute("profile", profile);
        model.addAttribute("profileForm", CustomerProfileForm.from(profile));

        return "customer/profile";
    }

    // Update the logged-in customer's profile
    @PostMapping("/customer/profile")
    public String updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileForm") CustomerProfileForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", customerService.current(authentication));
            return "customer/profile";
        }

        customerService.updateCurrent(authentication, form);

        redirectAttributes.addFlashAttribute(
                "success",
                "Profile updated successfully."
        );

        return "redirect:/customer/profile";
    }

    // Display all customers with optional search and pagination
    @GetMapping("/staff/customers")
    public String customers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<CustomerProfile> customers = customerService.search(q, page);

        model.addAttribute("customers", customers);
        model.addAttribute("q", q);

        return "customer/list";
    }

    // Display a specific customer's details
    @GetMapping("/staff/customers/{id}")
    public String customer(
            @PathVariable Long id,
            Model model) {

        CustomerProfile profile = customerService.get(id);

        model.addAttribute("profile", profile);

        return "customer/detail";
    }

    // Activate or deactivate a customer account
    @PostMapping("/admin/customers/{id}/toggle-active")
    public String toggleActive(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        customerService.toggleActive(id);

        redirectAttributes.addFlashAttribute(
                "success",
                "Customer account status updated."
        );

        return "redirect:/staff/customers/" + id;
    }
}