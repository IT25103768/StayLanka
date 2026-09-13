package com.staylanka.room;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms")
    public String rooms(@Valid @ModelAttribute("search") RoomSearchForm search,
                        BindingResult bindingResult,
                        @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Room> rooms;
        if (search.hasDates() && !bindingResult.hasErrors()) {
            try {
                rooms = roomService.available(search, page);
            } catch (BusinessRuleException ex) {
                bindingResult.reject("dates.invalid", ex.getMessage());
                rooms = Page.empty();
            }
        } else if (search.hasDates()) {
            rooms = Page.empty();
        } else {
            rooms = roomService.browsePublic(q, page);
        }
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomImages", roomService.primaryImagePaths(rooms.getContent()));
        model.addAttribute("roomTypes", roomService.activeTypes());
        model.addAttribute("q", q);
        return "room/search";
    }

    @GetMapping("/rooms/{id}")
    public String room(@PathVariable Long id, Model model) {
        model.addAttribute("room", roomService.getPublic(id));
        model.addAttribute("images", roomService.images(id));
        return "room/detail";
    }

    @GetMapping("/staff/rooms")
    public String manageRooms(@RequestParam(defaultValue = "") String q,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("rooms", roomService.browse(q, page));
        model.addAttribute("q", q);
        return "room/manage-list";
    }

    @GetMapping("/staff/rooms/new")
    public String newRoom(Model model) {
        prepareRoomForm(model, new RoomForm(), null);
        return "room/form";
    }

    @PostMapping("/staff/rooms")
    public String createRoom(@Valid @ModelAttribute RoomForm roomForm, BindingResult bindingResult,
                             Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareRoomForm(model, roomForm, null);
            return "room/form";
        }
        try {
            Room room = roomService.create(roomForm);
            redirectAttributes.addFlashAttribute("success", "Room created successfully.");
            return "redirect:/staff/rooms/" + room.getId() + "/edit";
        } catch (ConflictException ex) {
            bindingResult.rejectValue("roomNumber", "duplicate", ex.getMessage());
            prepareRoomForm(model, roomForm, null);
            return "room/form";
        }
    }

    @GetMapping("/staff/rooms/{id}/edit")
    public String editRoom(@PathVariable Long id, Model model) {
        Room room = roomService.get(id);
        prepareRoomForm(model, RoomForm.from(room), id);
        return "room/form";
    }

    @PostMapping("/staff/rooms/{id}")
    public String updateRoom(@PathVariable Long id, @Valid @ModelAttribute RoomForm roomForm,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareRoomForm(model, roomForm, id);
            return "room/form";
        }
        try {
            roomService.update(id, roomForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("roomNumber", "duplicate", ex.getMessage());
            prepareRoomForm(model, roomForm, id);
            return "room/form";
        }
        redirectAttributes.addFlashAttribute("success", "Room updated successfully.");
        return "redirect:/staff/rooms/" + id + "/edit";
    }

    @PostMapping("/staff/rooms/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roomService.deactivate(id);
        redirectAttributes.addFlashAttribute("success", "Room deactivated safely.");
        return "redirect:/staff/rooms";
    }

    @GetMapping("/admin/room-types")
    public String roomTypes(Model model) {
        model.addAttribute("roomTypes", roomService.allTypes());
        return "room/type-list";
    }

    @GetMapping("/admin/room-types/new")
    public String newType(Model model) {
        model.addAttribute("roomTypeForm", new RoomTypeForm());
        model.addAttribute("typeId", null);
        return "room/type-form";
    }

    @PostMapping("/admin/room-types")
    public String createType(@Valid @ModelAttribute RoomTypeForm roomTypeForm,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("typeId", null);
            return "room/type-form";
        }
        try {
            roomService.createType(roomTypeForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("typeId", null);
            return "room/type-form";
        }
        redirectAttributes.addFlashAttribute("success", "Room type created.");
        return "redirect:/admin/room-types";
    }

    @GetMapping("/admin/room-types/{id}/edit")
    public String editType(@PathVariable Long id, Model model) {
        model.addAttribute("roomTypeForm", RoomTypeForm.from(roomService.getType(id)));
        model.addAttribute("typeId", id);
        return "room/type-form";
    }

    @PostMapping("/admin/room-types/{id}")
    public String updateType(@PathVariable Long id, @Valid @ModelAttribute RoomTypeForm roomTypeForm,
                             BindingResult bindingResult, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("typeId", id);
            return "room/type-form";
        }
        try {
            roomService.updateType(id, roomTypeForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("typeId", id);
            return "room/type-form";
        }
        redirectAttributes.addFlashAttribute("success", "Room type updated.");
        return "redirect:/admin/room-types";
    }

    @PostMapping("/admin/room-types/{id}/toggle-active")
    public String toggleType(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roomService.toggleType(id);
        redirectAttributes.addFlashAttribute("success", "Room type status updated.");
        return "redirect:/admin/room-types";
    }

    private void prepareRoomForm(Model model, RoomForm form, Long id) {
        model.addAttribute("roomForm", form);
        model.addAttribute("roomTypes", roomService.allTypes());
        model.addAttribute("roomStatuses", RoomStatus.values());
        model.addAttribute("roomId", id);
        if (id != null) {
            model.addAttribute("images", roomService.images(id));
        }
    }
}
