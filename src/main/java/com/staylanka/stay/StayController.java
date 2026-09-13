package com.staylanka.stay;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.reservation.Reservation;
import com.staylanka.review.ReviewService;
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
public class StayController {
    private final StayService stayService;
    private final ReviewService reviewService;

    public StayController(StayService stayService, ReviewService reviewService) {
        this.stayService = stayService;
        this.reviewService = reviewService;
    }

    @GetMapping("/staff/check-ins")
    public String eligible(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reservations", stayService.eligibleForCheckIn(page));
        return "stay/check-in-list";
    }

    @GetMapping("/staff/check-ins/{reservationId}")
    public String checkInForm(@PathVariable Long reservationId, Model model) {
        Reservation reservation = stayService.eligibleReservation(reservationId);
        CheckInForm form = new CheckInForm();
        form.setGuestCount(reservation.getGuestCount());
        model.addAttribute("reservation", reservation);
        model.addAttribute("checkInForm", form);
        return "stay/check-in-form";
    }

    @PostMapping("/staff/check-ins/{reservationId}")
    public String checkIn(@PathVariable Long reservationId,
                          @Valid @ModelAttribute CheckInForm checkInForm,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("reservation", findEligible(reservationId));
            return "stay/check-in-form";
        }
        try {
            Stay stay = stayService.checkIn(reservationId, checkInForm);
            redirectAttributes.addFlashAttribute("success", "Guest checked in successfully.");
            return "redirect:/staff/stays/" + stay.getId();
        } catch (BusinessRuleException ex) {
            bindingResult.reject("checkin.invalid", ex.getMessage());
            model.addAttribute("reservation", findEligible(reservationId));
            return "stay/check-in-form";
        }
    }

    @GetMapping("/staff/stays")
    public String stays(@RequestParam(defaultValue = "false") boolean completed,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("stays", stayService.operational(completed, page));
        model.addAttribute("completed", completed);
        return "stay/list";
    }

    @GetMapping("/staff/stays/{id}")
    public String staffDetail(@PathVariable Long id, Model model) {
        prepareDetail(model, stayService.get(id));
        return "stay/detail";
    }

    @GetMapping("/customer/stays")
    public String customerHistory(Authentication authentication,
                                  @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("stays", stayService.ownHistory(authentication, page));
        return "stay/customer-list";
    }

    @GetMapping("/customer/stays/{id}")
    public String customerDetail(Authentication authentication, @PathVariable Long id, Model model) {
        Stay stay = stayService.own(authentication, id);
        prepareDetail(model, stay);
        model.addAttribute("canReview", reviewService.canReview(authentication, stay.getId()));
        return "stay/detail";
    }

    @PostMapping("/staff/stays/{stayId}/charges")
    public String addCharge(@PathVariable Long stayId,
                            @Valid @ModelAttribute AdditionalChargeForm additionalChargeForm,
                            BindingResult bindingResult, Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareDetail(model, stayService.get(stayId));
            return "stay/detail";
        }
        stayService.addCharge(stayId, additionalChargeForm);
        redirectAttributes.addFlashAttribute("success", "Additional charge added.");
        return "redirect:/staff/stays/" + stayId;
    }

    @GetMapping("/staff/stays/{stayId}/charges/{chargeId}/edit")
    public String editChargeForm(@PathVariable Long stayId, @PathVariable Long chargeId, Model model) {
        model.addAttribute("stay", stayService.get(stayId));
        model.addAttribute("chargeId", chargeId);
        model.addAttribute("additionalChargeForm", AdditionalChargeForm.from(stayService.getCharge(stayId, chargeId)));
        return "stay/charge-form";
    }

    @PostMapping("/staff/stays/{stayId}/charges/{chargeId}")
    public String updateCharge(@PathVariable Long stayId, @PathVariable Long chargeId,
                               @Valid @ModelAttribute AdditionalChargeForm additionalChargeForm,
                               BindingResult bindingResult, Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stay", stayService.get(stayId));
            model.addAttribute("chargeId", chargeId);
            return "stay/charge-form";
        }
        stayService.updateCharge(stayId, chargeId, additionalChargeForm);
        redirectAttributes.addFlashAttribute("success", "Additional charge updated.");
        return "redirect:/staff/stays/" + stayId;
    }

    @PostMapping("/staff/stays/{stayId}/charges/{chargeId}/delete")
    public String deleteCharge(@PathVariable Long stayId, @PathVariable Long chargeId,
                               RedirectAttributes redirectAttributes) {
        stayService.deleteCharge(stayId, chargeId);
        redirectAttributes.addFlashAttribute("success", "Additional charge removed.");
        return "redirect:/staff/stays/" + stayId;
    }

    @GetMapping("/staff/stays/{id}/check-out")
    public String checkOutForm(@PathVariable Long id, Model model) {
        model.addAttribute("stay", stayService.get(id));
        model.addAttribute("charges", stayService.charges(id));
        model.addAttribute("checkOutForm", new CheckOutForm());
        return "stay/check-out-form";
    }

    @PostMapping("/staff/stays/{id}/check-out")
    public String checkOut(@PathVariable Long id, @Valid @ModelAttribute CheckOutForm checkOutForm,
                           BindingResult bindingResult, Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stay", stayService.get(id));
            model.addAttribute("charges", stayService.charges(id));
            return "stay/check-out-form";
        }
        try {
            stayService.checkOut(id, checkOutForm);
        } catch (BusinessRuleException ex) {
            bindingResult.reject("checkout.invalid", ex.getMessage());
            model.addAttribute("stay", stayService.get(id));
            model.addAttribute("charges", stayService.charges(id));
            return "stay/check-out-form";
        }
        redirectAttributes.addFlashAttribute("success", "Guest checked out successfully.");
        return "redirect:/staff/stays/" + id;
    }

    private Reservation findEligible(Long reservationId) {
        return stayService.eligibleReservation(reservationId);
    }

    private void prepareDetail(Model model, Stay stay) {
        model.addAttribute("stay", stay);
        model.addAttribute("charges", stayService.charges(stay.getId()));
        model.addAttribute("additionalChargeForm", new AdditionalChargeForm());
        model.addAttribute("canReview", false);
    }
}
