package com.staylanka.common;

import com.staylanka.promotion.PromotionService;
import com.staylanka.review.ReviewService;
import com.staylanka.room.Room;
import com.staylanka.room.RoomService;
import com.staylanka.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {
    private final RoomService roomService;
    private final PromotionService promotionService;
    private final ReviewService reviewService;

    public HomeController(RoomService roomService, PromotionService promotionService, ReviewService reviewService) {
        this.roomService = roomService;
        this.promotionService = promotionService;
        this.reviewService = reviewService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Room> featuredRooms = roomService.browsePublic("", 0).getContent().stream().limit(3).toList();
        model.addAttribute("featuredRooms", featuredRooms);
        model.addAttribute("featuredRoomImages", roomService.primaryImagePaths(featuredRooms));
        model.addAttribute("roomTypes", roomService.activeTypes());
        model.addAttribute("activePromotions", promotionService.active(0).getContent().stream().limit(2).toList());
        model.addAttribute("featuredReviews", reviewService.approved(0).getContent().stream().limit(3).toList());
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN));
        boolean staff = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.STAFF));
        if (admin) {
            return "redirect:/admin/dashboard";
        }
        if (staff) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/customer/dashboard";
    }
}
