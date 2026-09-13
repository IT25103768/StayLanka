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

    @GetMapping("/promotions")
    public String active(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("promotions", promotionService.active(page));
        return "promotion/public-list";
    }

    @GetMapping("/admin/promotions")
    public String manage(Model model) {
        model.addAttribute("promotions", promotionService.all());
        return "promotion/manage-list";
    }

    @GetMapping("/admin/promotions/new")
    public String createForm(Model model) {
        prepare(model, new PromotionForm(), null);
        return "promotion/form";
    }

    @PostMapping("/admin/promotions")
    public String create(@Valid @ModelAttribute PromotionForm promotionForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepare(model, promotionForm, null);
            return "promotion/form";
        }
        try {
            promotionService.create(promotionForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("code", "duplicate", ex.getMessage());
            prepare(model, promotionForm, null);
            return "promotion/form";
        } catch (BusinessRuleException ex) {
            bindingResult.reject("promotion.invalid", ex.getMessage());
            prepare(model, promotionForm, null);
            return "promotion/form";
        }
        redirectAttributes.addFlashAttribute("success", "Promotion created.");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/admin/promotions/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        prepare(model, PromotionForm.from(promotionService.get(id)), id);
        return "promotion/form";
    }

    @PostMapping("/admin/promotions/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute PromotionForm promotionForm,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepare(model, promotionForm, id);
            return "promotion/form";
        }
        try {
            promotionService.update(id, promotionForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("code", "duplicate", ex.getMessage());
            prepare(model, promotionForm, id);
            return "promotion/form";
        } catch (BusinessRuleException ex) {
            bindingResult.reject("promotion.invalid", ex.getMessage());
            prepare(model, promotionForm, id);
            return "promotion/form";
        }
        redirectAttributes.addFlashAttribute("success", "Promotion updated.");
        return "redirect:/admin/promotions";
    }

    @PostMapping("/admin/promotions/{id}/toggle-active")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        promotionService.toggle(id);
        redirectAttributes.addFlashAttribute("success", "Promotion status updated.");
        return "redirect:/admin/promotions";
    }

    private void prepare(Model model, PromotionForm form, Long id) {
        model.addAttribute("promotionForm", form);
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionId", id);
    }
}
