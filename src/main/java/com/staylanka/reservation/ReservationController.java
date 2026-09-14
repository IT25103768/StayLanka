package com.staylanka.reservation;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.room.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@Controller
public class ReservationController {

    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService,
                                 RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    // =========================
    // CUSTOMER - LIST
    // =========================

    @GetMapping("/customer/reservations")
    public String ownReservations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int safePage = Math.max(page, 0);

        model.addAttribute(
                "reservations",
                reservationService.ownReservations(authentication, safePage)
        );

        return "reservation/customer-list";
    }

    // =========================
    // CUSTOMER - NEW
    // =========================

    @GetMapping("/customer/reservations/new")
    public String newReservation(
            @RequestParam Long roomId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            Model model) {

        ReservationForm form = new ReservationForm();

        form.setRoomId(roomId);
        form.setCheckInDate(checkIn);
        form.setCheckOutDate(checkOut);
        form.setGuestCount(Math.max(guests, 1));

        prepareForm(model, form, null);

        return "reservation/form";
    }

    // =========================
    // CUSTOMER - CREATE
    // =========================

    @PostMapping("/customer/reservations")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("reservationForm")
            ReservationForm reservationForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate form fields
        if (bindingResult.hasErrors()) {
            prepareForm(model, reservationForm, null);
            return "reservation/form";
        }

        // Validate reservation dates
        if (!validDates(reservationForm)) {
            bindingResult.reject(
                    "reservation.invalidDates",
                    "Check-out date must be after check-in date."
            );

            prepareForm(model, reservationForm, null);
            return "reservation/form";
        }

        try {
            Reservation reservation =
                    reservationService.create(authentication, reservationForm);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Reservation submitted successfully."
            );

            return "redirect:/customer/reservations/" + reservation.getId();

        } catch (BusinessRuleException | ConflictException ex) {

            bindingResult.reject(
                    "reservation.invalid",
                    safeMessage(ex, "Unable to create the reservation.")
            );

            prepareForm(model, reservationForm, null);

            return "reservation/form";
        }
    }

    // =========================
    // CUSTOMER - DETAIL
    // =========================

    @GetMapping("/customer/reservations/{id}")
    public String ownDetail(
            Authentication authentication,
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "reservation",
                reservationService.own(authentication, id)
        );

        return "reservation/detail";
    }

    // =========================
    // CUSTOMER - EDIT
    // =========================

    @GetMapping("/customer/reservations/{id}/edit")
    public String edit(
            Authentication authentication,
            @PathVariable Long id,
            Model model) {

        Reservation reservation =
                reservationService.own(authentication, id);

        prepareForm(
                model,
                ReservationForm.from(reservation),
                id
        );

        return "reservation/form";
    }

    // =========================
    // CUSTOMER - UPDATE
    // =========================

    @PostMapping("/customer/reservations/{id}")
    public String update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @ModelAttribute("reservationForm")
            ReservationForm reservationForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareForm(model, reservationForm, id);
            return "reservation/form";
        }

        // Validate reservation dates
        if (!validDates(reservationForm)) {
            bindingResult.reject(
                    "reservation.invalidDates",
                    "Check-out date must be after check-in date."
            );

            prepareForm(model, reservationForm, id);
            return "reservation/form";
        }

        try {

            reservationService.updateOwn(
                    authentication,
                    id,
                    reservationForm
            );

        } catch (BusinessRuleException | ConflictException ex) {

            bindingResult.reject(
                    "reservation.invalid",
                    safeMessage(ex, "Unable to update the reservation.")
            );

            prepareForm(model, reservationForm, id);

            return "reservation/form";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Reservation updated successfully."
        );

        return "redirect:/customer/reservations/" + id;
    }

    // =========================
    // CUSTOMER - CANCEL
    // =========================

    @PostMapping("/customer/reservations/{id}/cancel")
    public String cancel(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        String cleanedReason = cleanReason(reason);

        if (cleanedReason.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Cancellation reason is required."
            );

            return "redirect:/customer/reservations/" + id;
        }

        try {

            reservationService.cancelOwn(
                    authentication,
                    id,
                    cleanedReason
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Reservation cancelled successfully."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    safeMessage(ex, "Unable to cancel the reservation.")
            );
        }

        return "redirect:/customer/reservations/" + id;
    }

    // =========================
    // STAFF - LIST / SEARCH
    // =========================

    @GetMapping("/staff/reservations")
    public String manage(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int safePage = Math.max(page, 0);

        String searchQuery = q == null ? "" : q.trim();

        // Prevent invalid date range
        if (fromDate != null
                && toDate != null
                && toDate.isBefore(fromDate)) {

            model.addAttribute(
                    "searchError",
                    "To date must be on or after the from date."
            );
        }

        model.addAttribute(
                "reservations",
                reservationService.search(
                        searchQuery,
                        status,
                        fromDate,
                        toDate,
                        safePage
                )
        );

        model.addAttribute(
                "statuses",
                ReservationStatus.values()
        );

        model.addAttribute("q", searchQuery);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "reservation/manage-list";
    }

    // =========================
    // STAFF - DETAIL
    // =========================

    @GetMapping("/staff/reservations/{id}")
    public String staffDetail(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "reservation",
                reservationService.detailed(id)
        );

        return "reservation/detail";
    }

    // =========================
    // STAFF - CONFIRM
    // =========================

    @PostMapping("/staff/reservations/{id}/confirm")
    public String confirm(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            reservationService.confirm(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Reservation confirmed successfully."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    safeMessage(ex, "Unable to confirm the reservation.")
            );
        }

        return "redirect:/staff/reservations/" + id;
    }

    // =========================
    // STAFF - REJECT
    // =========================

    @PostMapping("/staff/reservations/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        String cleanedReason = cleanReason(reason);

        if (cleanedReason.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Rejection reason is required."
            );

            return "redirect:/staff/reservations/" + id;
        }

        try {

            reservationService.reject(
                    id,
                    cleanedReason
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Reservation rejected successfully."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    safeMessage(ex, "Unable to reject the reservation.")
            );
        }

        return "redirect:/staff/reservations/" + id;
    }

    // =========================
    // STAFF - NO SHOW
    // =========================

    @PostMapping("/staff/reservations/{id}/no-show")
    public String noShow(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {

        String cleanedReason = cleanReason(reason);

        try {

            reservationService.markNoShow(
                    id,
                    cleanedReason.isEmpty() ? null : cleanedReason
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Reservation marked as no-show."
            );

        } catch (BusinessRuleException | ConflictException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    safeMessage(ex, "Unable to mark the reservation as no-show.")
            );
        }

        return "redirect:/staff/reservations/" + id;
    }

    // =========================
    // HELPER METHODS
    // =========================

    private void prepareForm(
            Model model,
            ReservationForm form,
            Long reservationId) {

        model.addAttribute(
                "reservationForm",
                form
        );

        model.addAttribute(
                "reservationId",
                reservationId
        );

        if (form != null && form.getRoomId() != null) {

            model.addAttribute(
                    "room",
                    roomService.get(form.getRoomId())
            );

            model.addAttribute(
                    "roomImages",
                    roomService.images(form.getRoomId())
            );
        }
    }

    private boolean validDates(ReservationForm form) {

        if (form == null) {
            return false;
        }

        LocalDate checkIn = form.getCheckInDate();
        LocalDate checkOut = form.getCheckOutDate();

        if (checkIn == null || checkOut == null) {
            return false;
        }

        return checkOut.isAfter(checkIn);
    }

    private String cleanReason(String reason) {

        if (reason == null) {
            return "";
        }

        return reason.trim();
    }

    private String safeMessage(
            RuntimeException exception,
            String defaultMessage) {

        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return defaultMessage;
        }

        return exception.getMessage();
    }
}