package com.staylanka.customer;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.PurgeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class ProfileOperationsController {

    private final ProfileOperationsService operations;
    private final CustomerService customers;
    private final PurgeService purge;

    public ProfileOperationsController(
            ProfileOperationsService operations,
            CustomerService customers,
            PurgeService purge
    ) {
        this.operations = operations;
        this.customers = customers;
        this.purge = purge;
    }

    @GetMapping("/admin/customers/new")
    public String create(Model model) {

        model.addAttribute(
                "walkInForm",
                new WalkInForm()
        );

        return "customer/walk-in";
    }

    @PostMapping("/admin/customers/new")
    public String create(
            @Valid
            @ModelAttribute("walkInForm")
            WalkInForm walkInForm,

            BindingResult errors,

            RedirectAttributes flash
    ) {

        if (errors.hasErrors()) {
            return "customer/walk-in";
        }

        try {

            CustomerProfile profile =
                    operations.create(walkInForm);

            flash.addFlashAttribute(
                    "success",
                    "Walk-in profile created. " +
                            "Use verified recovery to enable guest sign-in."
            );

            return "redirect:/staff/customers/"
                    + profile.getId();

        } catch (BusinessRuleException |
                 ConflictException ex) {

            errors.reject(
                    "profile",
                    ex.getMessage()
            );

            return "customer/walk-in";
        }
    }

    @GetMapping("/admin/customers/{id}/edit")
    public String edit(
            @PathVariable("id") Long id,
            Model model
    ) {

        CustomerProfile profile =
                customers.get(id);

        model.addAttribute(
                "profileForm",
                CustomerProfileForm.from(profile)
        );

        model.addAttribute(
                "profileId",
                id
        );

        return "customer/staff-edit";
    }

    @PostMapping("/admin/customers/{id}/edit")
    public String edit(
            @PathVariable("id") Long id,

            @Valid
            @ModelAttribute("profileForm")
            CustomerProfileForm form,

            BindingResult errors,

            Model model,

            RedirectAttributes flash
    ) {

        model.addAttribute(
                "profileId",
                id
        );

        if (errors.hasErrors()) {
            return "customer/staff-edit";
        }

        operations.update(
                id,
                form
        );

        flash.addFlashAttribute(
                "success",
                "Profile updated."
        );

        return "redirect:/staff/customers/" + id;
    }

    @PostMapping("/admin/customers/{id}/anonymise")
    public String anonymise(
            @PathVariable("id") Long id,
            RedirectAttributes flash
    ) {

        operations.anonymise(id);

        flash.addFlashAttribute(
                "success",
                "Profile contact and identity fields " +
                        "anonymised. Historical operational " +
                        "notes remain subject to retention review."
        );

        return "redirect:/staff/customers/" + id;
    }

    /*
     * ======================================================
     * PERMANENT CUSTOMER DELETE
     * ======================================================
     *
     * ADMIN ONLY.
     *
     * PurgeService already checks:
     *
     * 1. Customer must be inactive.
     * 2. Customer must have no Reservation history.
     * 3. Customer must have no GuestRequest history.
     * 4. Customer must have no Review history.
     * 5. User must not be referenced by request history.
     * 6. User must not be referenced by request responses.
     *
     * It then removes:
     *
     * - notifications
     * - password recovery rows
     * - customer_profiles row
     * - app_users row
     *
     * and writes a HARD_DELETE audit event.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/customers/{id}/delete")
    public String deletePermanently(
            @PathVariable("id") Long id,

            @RequestParam("confirmation")
            String confirmation,

            RedirectAttributes flash
    ) {

        try {

            purge.delete(
                    "CustomerProfile",
                    id,
                    confirmation
            );

            flash.addFlashAttribute(
                    "success",
                    "Customer profile and login account " +
                            "permanently deleted from the database."
            );

            return "redirect:/staff/customers";

        } catch (BusinessRuleException ex) {

            flash.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

            return "redirect:/staff/customers/" + id;
        }
    }
}