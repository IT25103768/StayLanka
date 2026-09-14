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
    public String rooms(
            @Valid @ModelAttribute("search") RoomSearchForm search,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        page = normalizePage(page);

        Page<Room> rooms;

        if (search.hasDates()) {
            if (bindingResult.hasErrors()) {
                rooms = Page.empty();
            } else {
                try {
                    rooms = roomService.available(search, page);
                } catch (BusinessRuleException ex) {
                    bindingResult.reject("dates.invalid", ex.getMessage());
                    rooms = Page.empty();
                }
            }
        } else {
            rooms = roomService.browsePublic(q.trim(), page);
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute(
                "roomImages",
                roomService.primaryImagePaths(rooms.getContent())
        );
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
    public String manageRooms(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        page = normalizePage(page);

        model.addAttribute("rooms", roomService.browse(q.trim(), page));
        model.addAttribute("q", q);

        return "room/manage-list";
    }

    @GetMapping("/staff/rooms/new")
    public String newRoom(Model model) {

        prepareRoomForm(model, new RoomForm(), null);

        return "room/form";
    }

    @PostMapping("/staff/rooms")
    public String createRoom(
            @Valid @ModelAttribute("roomForm") RoomForm roomForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareRoomForm(model, roomForm, null);
            return "room/form";
        }

        try {
            Room room = roomService.create(roomForm);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room created successfully."
            );

            return "redirect:/staff/rooms/" + room.getId() + "/edit";

        } catch (ConflictException ex) {

            bindingResult.rejectValue(
                    "roomNumber",
                    "duplicate",
                    ex.getMessage()
            );

            prepareRoomForm(model, roomForm, null);
            return "room/form";

        } catch (BusinessRuleException ex) {

            bindingResult.reject(
                    "room.invalid",
                    ex.getMessage()
            );

            prepareRoomForm(model, roomForm, null);
            return "room/form";
        }
    }

    @GetMapping("/staff/rooms/{id}/edit")
    public String editRoom(
            @PathVariable Long id,
            Model model) {

        Room room = roomService.get(id);

        prepareRoomForm(
                model,
                RoomForm.from(room),
                id
        );

        return "room/form";
    }

    @PostMapping("/staff/rooms/{id}")
    public String updateRoom(
            @PathVariable Long id,
            @Valid @ModelAttribute("roomForm") RoomForm roomForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareRoomForm(model, roomForm, id);
            return "room/form";
        }

        try {
            roomService.update(id, roomForm);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room updated successfully."
            );

            return "redirect:/staff/rooms/" + id + "/edit";

        } catch (ConflictException ex) {

            bindingResult.rejectValue(
                    "roomNumber",
                    "duplicate",
                    ex.getMessage()
            );

            prepareRoomForm(model, roomForm, id);
            return "room/form";

        } catch (BusinessRuleException ex) {

            bindingResult.reject(
                    "room.invalid",
                    ex.getMessage()
            );

            prepareRoomForm(model, roomForm, id);
            return "room/form";
        }
    }

    @PostMapping("/staff/rooms/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            roomService.deactivate(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room deactivated safely."
            );

        } catch (BusinessRuleException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/staff/rooms";
    }

    @PostMapping("/staff/rooms/{id}/status")
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam RoomStatus status,
            RedirectAttributes redirectAttributes) {

        try {

            roomService.changeOperationalStatus(id, status);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room status updated successfully."
            );

        } catch (BusinessRuleException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/staff/rooms";
    }

    @GetMapping("/admin/room-types")
    public String roomTypes(Model model) {

        model.addAttribute(
                "roomTypes",
                roomService.allTypes()
        );

        return "room/type-list";
    }

    @GetMapping("/admin/room-types/new")
    public String newType(Model model) {

        model.addAttribute(
                "roomTypeForm",
                new RoomTypeForm()
        );

        model.addAttribute("typeId", null);

        return "room/type-form";
    }

    @PostMapping("/admin/room-types")
    public String createType(
            @Valid @ModelAttribute("roomTypeForm") RoomTypeForm roomTypeForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            model.addAttribute("typeId", null);

            return "room/type-form";
        }

        try {

            roomService.createType(roomTypeForm);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room type created."
            );

            return "redirect:/admin/room-types";

        } catch (ConflictException ex) {

            bindingResult.rejectValue(
                    "name",
                    "duplicate",
                    ex.getMessage()
            );

            model.addAttribute("typeId", null);

            return "room/type-form";
        }
    }

    @GetMapping("/admin/room-types/{id}/edit")
    public String editType(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "roomTypeForm",
                RoomTypeForm.from(roomService.getType(id))
        );

        model.addAttribute("typeId", id);

        return "room/type-form";
    }

    @PostMapping("/admin/room-types/{id}")
    public String updateType(
            @PathVariable Long id,
            @Valid @ModelAttribute("roomTypeForm") RoomTypeForm roomTypeForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            model.addAttribute("typeId", id);

            return "room/type-form";
        }

        try {

            roomService.updateType(id, roomTypeForm);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room type updated."
            );

            return "redirect:/admin/room-types";

        } catch (ConflictException ex) {

            bindingResult.rejectValue(
                    "name",
                    "duplicate",
                    ex.getMessage()
            );

            model.addAttribute("typeId", id);

            return "room/type-form";

        } catch (BusinessRuleException ex) {

            bindingResult.reject(
                    "roomType.invalid",
                    ex.getMessage()
            );

            model.addAttribute("typeId", id);

            return "room/type-form";
        }
    }

    @PostMapping("/admin/room-types/{id}/toggle-active")
    public String toggleType(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            roomService.toggleType(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Room type status updated."
            );

        } catch (BusinessRuleException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/room-types";
    }

    private void prepareRoomForm(
            Model model,
            RoomForm form,
            Long id) {

        model.addAttribute("roomForm", form);

        if (id == null) {
            model.addAttribute(
                    "roomTypes",
                    roomService.activeTypes()
            );
        } else {
            model.addAttribute(
                    "roomTypes",
                    roomService.allTypes()
            );

            model.addAttribute(
                    "images",
                    roomService.images(id)
            );
        }

        model.addAttribute("roomId", id);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }
}