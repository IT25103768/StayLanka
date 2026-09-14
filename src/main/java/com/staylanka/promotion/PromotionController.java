package com.staylanka.promotion;

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
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * Displays active promotions for public users.
     */
    @GetMapping("/promotions")
    public String active(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        if (page < 0) {
            page = 0;
        }

        model.addAttribute("promotions", promotionService.active(page));

        return "promotion/public-list";
    }

    /**
     * Displays all promotions for administrators.
     */
    @GetMapping("/admin/promotions")
    public String manage(Model model) {
        model.addAttribute("promotions", promotionService.all());

        return "promotion/manage-list";
    }

    /**
     * Displays the promotion creation form.
     */
    @GetMapping("/admin/promotions/new")
    public String createForm(Model model) {
        prepareForm(model, new PromotionForm(), null);

        return "promotion/form";
    }

    /**
     * Creates a new promotion.
     */
    @PostMapping("/admin/promotions")
    public String create(
            @Valid @ModelAttribute("promotionForm") PromotionForm promotionForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, promotionForm, null);
            return "promotion/form";
        }

        try {
            promotionService.create(promotionForm);

        } catch (ConflictException ex) {
            bindingResult.rejectValue(
                    "code",
                    "duplicate",
                    ex.getMessage()
            );

            prepareForm(model, promotionForm, null);
            return "promotion/form";

        } catch (BusinessRuleException ex) {
            bindingResult.reject(
                    "promotion.invalid",
                    ex.getMessage()
            );

            prepareForm(model, promotionForm, null);
            return "promotion/form";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Promotion created successfully."
        );

        return "redirect:/admin/promotions";
    }

    /**
     * Displays the promotion edit form.
     */
    @GetMapping("/admin/promotions/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model
    ) {
        if (id == null || id <= 0) {
            return "redirect:/admin/promotions";
        }

        Promotion promotion = promotionService.get(id);

        prepareForm(
                model,
                PromotionForm.from(promotion),
                id
        );

        return "promotion/form";
    }

    /**
     * Updates an existing promotion.
     */
    @PostMapping("/admin/promotions/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("promotionForm") PromotionForm promotionForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (id == null || id <= 0) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Invalid promotion."
            );

            return "redirect:/admin/promotions";
        }

        if (bindingResult.hasErrors()) {
            prepareForm(model, promotionForm, id);
            return "promotion/form";
        }

        try {
            promotionService.update(id, promotionForm);

        } catch (ConflictException ex) {
            bindingResult.rejectValue(
                    "code",
                    "duplicate",
                    ex.getMessage()
            );

            prepareForm(model, promotionForm, id);
            return "promotion/form";

        } catch (BusinessRuleException ex) {
            bindingResult.reject(
                    "promotion.invalid",
                    ex.getMessage()
            );

            prepareForm(model, promotionForm, id);
            return "promotion/form";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Promotion updated successfully."
        );

        return "redirect:/admin/promotions";
    }

    /**
     * Activates or deactivates a promotion.
     */
    @PostMapping("/admin/promotions/{id}/toggle-active")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        if (id == null || id <= 0) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Invalid promotion."
            );

            return "redirect:/admin/promotions";
        }

        try {
            promotionService.toggle(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Promotion status updated successfully."
            );

        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/promotions";
    }

    /**
     * Prepares common model attributes required by the promotion form.
     */
    private void prepareForm(
            Model model,
            PromotionForm form,
            Long id
    ) {
        model.addAttribute("promotionForm", form);
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionId", id);
    }
}