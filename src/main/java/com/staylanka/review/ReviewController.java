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

    private static final int DEFAULT_PAGE = 0;

    private final ReviewService reviewService;
    private final StayService stayService;

    public ReviewController(
            ReviewService reviewService,
            StayService stayService
    ) {
        this.reviewService = reviewService;
        this.stayService = stayService;
    }

    /**
     * Displays approved reviews publicly.
     */
    @GetMapping("/reviews")
    public String publicReviews(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "reviews",
                reviewService.approved(safePage(page))
        );

        return "review/public-list";
    }

    /**
     * Displays reviews created by the authenticated customer.
     */
    @GetMapping("/customer/reviews")
    public String own(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute(
                "reviews",
                reviewService.own(authentication, safePage(page))
        );

        return "review/customer-list";
    }

    /**
     * Displays the form for creating a new review.
     */
    @GetMapping("/customer/reviews/new")
    public String createForm(
            Authentication authentication,
            @RequestParam Long stayId,
            Model model
    ) {
        validateId(stayId, "Stay ID");

        if (!reviewService.canReview(authentication, stayId)) {
            throw new BusinessRuleException(
                    "This stay is not eligible for a new review."
            );
        }

        prepareForm(
                model,
                stayService.own(authentication, stayId),
                stayId,
                null,
                new ReviewForm()
        );

        return "review/form";
    }

    /**
     * Creates a new review.
     */
    @PostMapping("/customer/reviews")
    public String create(
            Authentication authentication,
            @RequestParam Long stayId,
            @Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validateId(stayId, "Stay ID");

        if (bindingResult.hasErrors()) {
            prepareFormWithStay(
                    authentication,
                    stayId,
                    model,
                    null,
                    reviewForm
            );

            return "review/form";
        }

        try {
            reviewService.create(
                    authentication,
                    stayId,
                    reviewForm
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Review submitted for moderation."
            );

            return "redirect:/customer/reviews";

        } catch (BusinessRuleException | ConflictException ex) {

            bindingResult.reject(
                    "review.invalid",
                    ex.getMessage()
            );

            prepareFormWithStay(
                    authentication,
                    stayId,
                    model,
                    null,
                    reviewForm
            );

            return "review/form";
        }
    }

    /**
     * Displays the edit form for an existing customer review.
     */
    @GetMapping("/customer/reviews/{id}/edit")
    public String editForm(
            Authentication authentication,
            @PathVariable Long id,
            Model model
    ) {
        validateId(id, "Review ID");

        Review review = reviewService.ownReview(
                authentication,
                id
        );

        prepareForm(
                model,
                review.getStay(),
                review.getStay().getId(),
                id,
                ReviewForm.from(review)
        );

        return "review/form";
    }

    /**
     * Updates an existing customer review.
     */
    @PostMapping("/customer/reviews/{id}")
    public String update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validateId(id, "Review ID");

        if (bindingResult.hasErrors()) {
            Review review = reviewService.ownReview(
                    authentication,
                    id
            );

            prepareForm(
                    model,
                    review.getStay(),
                    review.getStay().getId(),
                    id,
                    reviewForm
            );

            return "review/form";
        }

        try {
            reviewService.update(
                    authentication,
                    id,
                    reviewForm
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Review updated and returned to moderation."
            );

            return "redirect:/customer/reviews";

        } catch (BusinessRuleException | ConflictException ex) {

            bindingResult.reject(
                    "review.invalid",
                    ex.getMessage()
            );

            Review review = reviewService.ownReview(
                    authentication,
                    id
            );

            prepareForm(
                    model,
                    review.getStay(),
                    review.getStay().getId(),
                    id,
                    reviewForm
            );

            return "review/form";
        }
    }

    /**
     * Deletes a customer's review.
     */
    @PostMapping("/customer/reviews/{id}/delete")
    public String delete(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        validateId(id, "Review ID");

        try {
            reviewService.delete(
                    authentication,
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Review deleted."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/customer/reviews";
    }

    /**
     * Displays the review moderation page.
     */
    @GetMapping("/admin/reviews")
    public String moderation(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        ReviewStatus selectedStatus =
                status == null
                        ? ReviewStatus.PENDING
                        : status;

        model.addAttribute(
                "reviews",
                reviewService.moderate(
                        selectedStatus,
                        safePage(page)
                )
        );

        model.addAttribute(
                "statuses",
                ReviewStatus.values()
        );

        model.addAttribute(
                "selectedStatus",
                selectedStatus
        );

        return "review/moderation-list";
    }

    /**
     * Approves a review.
     */
    @PostMapping("/admin/reviews/{id}/approve")
    public String approve(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        validateId(id, "Review ID");

        try {
            reviewService.approve(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Review approved."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/reviews";
    }

    /**
     * Rejects a review.
     */
    @PostMapping("/admin/reviews/{id}/reject")
    public String reject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        validateId(id, "Review ID");

        try {
            reviewService.reject(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Review rejected."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/reviews";
    }

    /**
     * Prepares common review form attributes.
     */
    private void prepareForm(
            Model model,
            Object stay,
            Long stayId,
            Long reviewId,
            ReviewForm reviewForm
    ) {
        model.addAttribute("stay", stay);
        model.addAttribute("stayId", stayId);
        model.addAttribute("reviewId", reviewId);
        model.addAttribute("reviewForm", reviewForm);
    }

    /**
     * Loads the customer's stay and prepares the review form.
     */
    private void prepareFormWithStay(
            Authentication authentication,
            Long stayId,
            Model model,
            Long reviewId,
            ReviewForm reviewForm
    ) {
        prepareForm(
                model,
                stayService.own(authentication, stayId),
                stayId,
                reviewId,
                reviewForm
        );
    }

    /**
     * Prevents negative pagination values.
     */
    private int safePage(int page) {
        return Math.max(page, DEFAULT_PAGE);
    }

    /**
     * Validates entity IDs.
     */
    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a positive number."
            );
        }
    }
}