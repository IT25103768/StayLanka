package com.staylanka.stay;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class StayOperationsController {
    private final StayService stays;
    public StayOperationsController(StayService stays){this.stays=stays;}
    @PostMapping("/staff/stays/{id}/move") public String move(@PathVariable Long id,@RequestParam Long roomId,RedirectAttributes flash){stays.move(id,roomId);flash.addFlashAttribute("success","Room assignment updated; original contracted rate retained.");return "redirect:/staff/stays/"+id;}
    @PostMapping("/staff/stays/{id}/extend") public String extend(@PathVariable Long id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate departure,RedirectAttributes flash){stays.extend(id,departure);flash.addFlashAttribute("success","Stay extended at the original nightly rate; existing discount retained without discounting extra nights.");return "redirect:/staff/stays/"+id;}
    @PostMapping("/staff/stays/{id}/void") public String voidStay(@PathVariable Long id,RedirectAttributes flash){stays.voidStay(id);flash.addFlashAttribute("success","Erroneous stay voided and reservation cancelled; history retained.");return "redirect:/staff/stays/"+id;}
}
