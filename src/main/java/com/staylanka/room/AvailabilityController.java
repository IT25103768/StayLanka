package com.staylanka.room;

import com.staylanka.reservation.ReservationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.ArrayList;

@Controller
public class AvailabilityController {
    @org.springframework.beans.factory.annotation.Autowired private MaintenanceService maintenance;
    private final RoomService rooms; private final ReservationRepository bookings;
    public AvailabilityController(RoomService rooms,ReservationRepository bookings){this.rooms=rooms;this.bookings=bookings;}
    public record Day(LocalDate date,String state){}
    @GetMapping("/rooms/{id}/calendar") @Transactional(readOnly=true)
    public String calendar(@PathVariable Long id,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,Model model){
        Room room=rooms.getPublic(id);LocalDate start=from==null?LocalDate.now():from;
        var days=new ArrayList<Day>();
        for(int i=0;i<31;i++){LocalDate date=start.plusDays(i);String state=bookings.countBlockingOverlaps(id,date,date.plusDays(1),null)>0?"Booked":"Available";if(room.getStatus()==RoomStatus.OCCUPIED&&date.equals(LocalDate.now()))state="Occupied";if(maintenance.blocked(id,date,date.plusDays(1)))state="Maintenance";days.add(new Day(date,state));}
        model.addAttribute("room",room);model.addAttribute("days",days);model.addAttribute("from",start);return "room/calendar";
    }
}
