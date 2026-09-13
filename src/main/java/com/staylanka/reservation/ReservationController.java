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

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    @GetMapping("/customer/reservations")
    public String ownReservations(Authentication authentication,
                                  @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reservations", reservationService.ownReservations(authentication, page));
        return "reservation/customer-list";
    }

    @GetMapping("/customer/reservations/new")
    public String newReservation(@RequestParam Long roomId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                                 @RequestParam(defaultValue = "1") int guests, Model model) {
        ReservationForm form = new ReservationForm();
        form.setRoomId(roomId);
        form.setCheckInDate(checkIn);
        form.setCheckOutDate(checkOut);
        form.setGuestCount(Math.max(guests, 1));
        prepareForm(model, form, null);
        return "reservation/form";
    }

    @PostMapping("/customer/reservations")
    public String create(Authentication authentication,
                         @Valid @ModelAttribute ReservationForm reservationForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, reservationForm, null);
            return "reservation/form";
        }
        try {
            Reservation reservation = reservationService.create(authentication, reservationForm);
            redirectAttributes.addFlashAttribute("success", "Reservation submitted successfully.");
            return "redirect:/customer/reservations/" + reservation.getId();
        } catch (BusinessRuleException | ConflictException ex) {
            bindingResult.reject("reservation.invalid", ex.getMessage());
            prepareForm(model, reservationForm, null);
            return "reservation/form";
        }
    }

    @GetMapping("/customer/reservations/{id}")
    public String ownDetail(Authentication authentication, @PathVariable Long id, Model model) {
        model.addAttribute("reservation", reservationService.own(authentication, id));
        return "reservation/detail";
    }

    @GetMapping("/customer/reservations/{id}/edit")
    public String edit(Authentication authentication, @PathVariable Long id, Model model) {
        Reservation reservation = reservationService.own(authentication, id);
        prepareForm(model, ReservationForm.from(reservation), id);
        return "reservation/form";
    }

    @PostMapping("/customer/reservations/{id}")
    public String update(Authentication authentication, @PathVariable Long id,
                         @Valid @ModelAttribute ReservationForm reservationForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, reservationForm, id);
            return "reservation/form";
        }
        try {
            reservationService.updateOwn(authentication, id, reservationForm);
        } catch (BusinessRuleException | ConflictException ex) {
            bindingResult.reject("reservation.invalid", ex.getMessage());
            prepareForm(model, reservationForm, id);
            return "reservation/form";
        }
        redirectAttributes.addFlashAttribute("success", "Reservation updated.");
        return "redirect:/customer/reservations/" + id;
    }

    @PostMapping("/customer/reservations/{id}/cancel")
    public String cancel(Authentication authentication, @PathVariable Long id,
                         @RequestParam String reason, RedirectAttributes redirectAttributes) {
        reservationService.cancelOwn(authentication, id, reason);
        redirectAttributes.addFlashAttribute("success", "Reservation cancelled.");
        return "redirect:/customer/reservations/" + id;
    }

    @GetMapping("/staff/reservations")
    public String manage(@RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) ReservationStatus status,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reservations", reservationService.search(q, status, fromDate, toDate, page));
        model.addAttribute("statuses", ReservationStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "reservation/manage-list";
    }

    @GetMapping("/staff/reservations/{id}")
    public String staffDetail(@PathVariable Long id, Model model) {
        model.addAttribute("reservation", reservationService.detailed(id));
        return "reservation/detail";
    }

    @PostMapping("/staff/reservations/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reservationService.confirm(id);
        redirectAttributes.addFlashAttribute("success", "Reservation confirmed.");
        return "redirect:/staff/reservations/" + id;
    }

    @PostMapping("/staff/reservations/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam String reason,
                         RedirectAttributes redirectAttributes) {
        reservationService.reject(id, reason);
        redirectAttributes.addFlashAttribute("success", "Reservation rejected.");
        return "redirect:/staff/reservations/" + id;
    }

    @PostMapping("/staff/reservations/{id}/no-show")
    public String noShow(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        reservationService.markNoShow(id, reason);
        redirectAttributes.addFlashAttribute("success", "Reservation marked as no-show.");
        return "redirect:/staff/reservations/" + id;
    }

    private void prepareForm(Model model, ReservationForm form, Long reservationId) {
        model.addAttribute("reservationForm", form);
        model.addAttribute("reservationId", reservationId);
        if (form.getRoomId() != null) {
            model.addAttribute("room", roomService.get(form.getRoomId()));
            model.addAttribute("roomImages", roomService.images(form.getRoomId()));
        }
    }
}
