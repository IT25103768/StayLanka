package com.staylanka.room;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
@Controller
public class MaintenanceController {
    private final MaintenanceService maintenance;private final RoomService rooms;
    public MaintenanceController(MaintenanceService maintenance,RoomService rooms){this.maintenance=maintenance;this.rooms=rooms;}
    @GetMapping("/staff/rooms/maintenance")public String page(Model model){model.addAttribute("blocks",maintenance.all());model.addAttribute("rooms",rooms.selection());return "room/maintenance";}
    @PostMapping("/staff/rooms/maintenance")public String create(@RequestParam Long roomId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate start,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate end,@RequestParam String reason,RedirectAttributes flash){maintenance.create(roomId,start,end,reason);flash.addFlashAttribute("success","Maintenance scheduled. These dates are no longer bookable.");return "redirect:/staff/rooms/maintenance";}
    @PostMapping("/staff/rooms/maintenance/{id}/release")public String release(@PathVariable Long id,RedirectAttributes flash){maintenance.release(id);flash.addFlashAttribute("success","Maintenance block released.");return "redirect:/staff/rooms/maintenance";}
}
