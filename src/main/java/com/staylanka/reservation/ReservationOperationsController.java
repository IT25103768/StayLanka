package com.staylanka.reservation;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.room.RoomService;
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
public class ReservationOperationsController {

    private final ReservationService reservations;
    private final RoomService rooms;

    public ReservationOperationsController(
            ReservationService reservations,
            RoomService rooms
    ) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    @GetMapping("/staff/reservations/{id}/edit")
    public String edit(
            @PathVariable("id") Long id,
            Model model
    ) {
        Reservation booking = reservations.detailed(id);

        model.addAttribute(
                "reservationForm",
                ReservationForm.from(booking)
        );

        prepare(
                id,
                booking.getRoom().getId(),
                model
        );

        return "reservation/form";
    }

    @PostMapping("/staff/reservations/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("reservationForm") ReservationForm reservationForm,
            BindingResult errors,
            Model model,
            RedirectAttributes flash
    ) {
        if (!errors.hasErrors()) {
            try {
                reservations.updateStaff(id, reservationForm);

                flash.addFlashAttribute(
                        "success",
                        "Reservation updated."
                );

                return "redirect:/staff/reservations/" + id;

            } catch (BusinessRuleException | ConflictException ex) {
                errors.reject(
                        "booking",
                        ex.getMessage()
                );
            }
        }

        prepare(
                id,
                reservationForm.getRoomId(),
                model
        );

        return "reservation/form";
    }

    @PostMapping("/staff/reservations/{id}/cancel")
    public String cancel(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            RedirectAttributes flash
    ) {
        try {
            reservations.cancelStaff(id, reason);

            flash.addFlashAttribute(
                    "success",
                    "Reservation cancelled."
            );

        } catch (BusinessRuleException ex) {
            flash.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/staff/reservations/" + id;
    }

    /**
     * Real CRUD DELETE for Reservation Management.
     *
     * Customers cannot call this route because it is under /staff/**
     * and ReservationService also protects the operation with
     * RESERVATION_MANAGER / ADMIN method security.
     */
    @PostMapping("/staff/reservations/{id}/delete")
    public String deletePermanently(
            @PathVariable("id") Long id,
            RedirectAttributes flash
    ) {
        try {
            reservations.deletePermanently(id);

            flash.addFlashAttribute(
                    "success",
                    "Reservation permanently deleted from the database."
            );

            return "redirect:/staff/reservations";

        } catch (BusinessRuleException ex) {
            flash.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

            return "redirect:/staff/reservations/" + id;
        }
    }

    private void prepare(
            Long id,
            Long roomId,
            Model model
    ) {
        model.addAttribute(
                "reservationId",
                id
        );

        model.addAttribute(
                "staffEdit",
                true
        );

        model.addAttribute(
                "room",
                rooms.get(roomId)
        );

        model.addAttribute(
                "roomImages",
                rooms.images(roomId)
        );

        model.addAttribute(
                "roomChoices",
                rooms.selection()
        );
    }
}