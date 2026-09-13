package com.staylanka.review;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.stay.StayService;
import jakarta.validation.Valid;
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
public class ReviewController {
    private final ReviewService reviewService;
    private final StayService stayService;

    public ReviewController(ReviewService reviewService, StayService stayService) {
        this.reviewService = reviewService;
        this.stayService = stayService;
    }

    @GetMapping("/reviews")
    public String publicReviews(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reviews", reviewService.approved(page));
        return "review/public-list";
    }

    @GetMapping("/customer/reviews")
    public String own(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reviews", reviewService.own(authentication, page));
        return "review/customer-list";
    }

    @GetMapping("/customer/reviews/new")
    public String createForm(Authentication authentication, @RequestParam Long stayId, Model model) {
        if (!reviewService.canReview(authentication, stayId)) {
            throw new BusinessRuleException("This stay is not eligible for a new review.");
        }
        model.addAttribute("stay", stayService.own(authentication, stayId));
        model.addAttribute("stayId", stayId);
        model.addAttribute("reviewId", null);
        model.addAttribute("reviewForm", new ReviewForm());
        return "review/form";
    }

    @PostMapping("/customer/reviews")
    public String create(Authentication authentication, @RequestParam Long stayId,
                         @Valid @ModelAttribute ReviewForm reviewForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stay", stayService.own(authentication, stayId));
            model.addAttribute("stayId", stayId);
            model.addAttribute("reviewId", null);
            return "review/form";
        }
        try {
            reviewService.create(authentication, stayId, reviewForm);
        } catch (BusinessRuleException | ConflictException ex) {
            bindingResult.reject("review.invalid", ex.getMessage());
            model.addAttribute("stay", stayService.own(authentication, stayId));
            model.addAttribute("stayId", stayId);
            model.addAttribute("reviewId", null);
            return "review/form";
        }
        redirectAttributes.addFlashAttribute("success", "Review submitted for moderation.");
        return "redirect:/customer/reviews";
    }

    @GetMapping("/customer/reviews/{id}/edit")
    public String editForm(Authentication authentication, @PathVariable Long id, Model model) {
        Review review = reviewService.ownReview(authentication, id);
        model.addAttribute("stay", review.getStay());
        model.addAttribute("stayId", review.getStay().getId());
        model.addAttribute("reviewId", id);
        model.addAttribute("reviewForm", ReviewForm.from(review));
        return "review/form";
    }

    @PostMapping("/customer/reviews/{id}")
    public String update(Authentication authentication, @PathVariable Long id,
                         @Valid @ModelAttribute ReviewForm reviewForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Review review = reviewService.ownReview(authentication, id);
            model.addAttribute("stay", review.getStay());
            model.addAttribute("stayId", review.getStay().getId());
            model.addAttribute("reviewId", id);
            return "review/form";
        }
        reviewService.update(authentication, id, reviewForm);
        redirectAttributes.addFlashAttribute("success", "Review updated and returned to moderation.");
        return "redirect:/customer/reviews";
    }

    @PostMapping("/customer/reviews/{id}/delete")
    public String delete(Authentication authentication, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        reviewService.delete(authentication, id);
        redirectAttributes.addFlashAttribute("success", "Review deleted.");
        return "redirect:/customer/reviews";
    }

    @GetMapping("/admin/reviews")
    public String moderation(@RequestParam(required = false) ReviewStatus status,
                             @RequestParam(defaultValue = "0") int page, Model model) {
        ReviewStatus selected = status == null ? ReviewStatus.PENDING : status;
        model.addAttribute("reviews", reviewService.moderate(selected, page));
        model.addAttribute("statuses", ReviewStatus.values());
        model.addAttribute("selectedStatus", selected);
        return "review/moderation-list";
    }

    @PostMapping("/admin/reviews/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.approve(id);
        redirectAttributes.addFlashAttribute("success", "Review approved.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/admin/reviews/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.reject(id);
        redirectAttributes.addFlashAttribute("success", "Review rejected.");
        return "redirect:/admin/reviews";
    }
}
