package com.staylanka.common;

import com.staylanka.request.GuestRequestService;
import com.staylanka.reservation.ReservationService;
import com.staylanka.review.ReviewService;
import com.staylanka.room.RoomService;
import com.staylanka.stay.StayService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final StayService stayService;
    private final GuestRequestService requestService;
    private final ReviewService reviewService;

    public DashboardController(RoomService roomService, ReservationService reservationService,
                               StayService stayService, GuestRequestService requestService,
                               ReviewService reviewService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.stayService = stayService;
        this.requestService = requestService;
        this.reviewService = reviewService;
    }

    @GetMapping({"/admin/dashboard", "/staff/dashboard"})
    public String operations(Model model) {
        model.addAttribute("availableRooms", roomService.countAvailable());
        model.addAttribute("pendingReservations", reservationService.pendingCount());
        model.addAttribute("currentStays", stayService.currentCount());
        model.addAttribute("openRequests", requestService.openCount());
        model.addAttribute("pendingReviews", reviewService.pendingCount());
        return "dashboard/operations";
    }

    @GetMapping("/customer/dashboard")
    public String customer(Authentication authentication, Model model) {
        model.addAttribute("reservations", reservationService.ownReservations(authentication, 0).getContent().stream().limit(5).toList());
        model.addAttribute("stays", stayService.ownHistory(authentication, 0).getContent().stream().limit(5).toList());
        model.addAttribute("requests", requestService.ownRequests(authentication, 0).getContent().stream().limit(5).toList());
        return "dashboard/customer";
    }
}
