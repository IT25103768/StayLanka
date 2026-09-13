package com.staylanka.request;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.reservation.ReservationService;
import jakarta.validation.Valid;
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

@Controller
public class GuestRequestController {
    private final GuestRequestService requestService;
    private final ReservationService reservationService;

    public GuestRequestController(GuestRequestService requestService, ReservationService reservationService) {
        this.requestService = requestService;
        this.reservationService = reservationService;
    }

    @GetMapping("/customer/requests")
    public String own(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", requestService.ownRequests(authentication, page));
        return "request/customer-list";
    }

    @GetMapping("/customer/requests/new")
    public String createForm(Authentication authentication,
                             @RequestParam(required = false) Long reservationId, Model model) {
        GuestRequestForm form = new GuestRequestForm();
        form.setReservationId(reservationId);
        prepareCustomerForm(authentication, model, form, null);
        return "request/form";
    }

    @PostMapping("/customer/requests")
    public String create(Authentication authentication,
                         @Valid @ModelAttribute GuestRequestForm guestRequestForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareCustomerForm(authentication, model, guestRequestForm, null);
            return "request/form";
        }
        try {
            GuestRequest request = requestService.create(authentication, guestRequestForm);
            redirectAttributes.addFlashAttribute("success", "Request submitted successfully.");
            return "redirect:/customer/requests/" + request.getId();
        } catch (BusinessRuleException | ConflictException ex) {
            bindingResult.reject("request.invalid", ex.getMessage());
            prepareCustomerForm(authentication, model, guestRequestForm, null);
            return "request/form";
        }
    }

    @GetMapping("/customer/requests/{id}")
    public String customerDetail(Authentication authentication, @PathVariable Long id, Model model) {
        GuestRequest request = requestService.own(authentication, id);
        prepareDetail(model, request, false);
        return "request/detail";
    }

    @GetMapping("/customer/requests/{id}/edit")
    public String editForm(Authentication authentication, @PathVariable Long id, Model model) {
        GuestRequest request = requestService.own(authentication, id);
        prepareCustomerForm(authentication, model, GuestRequestForm.from(request), id);
        return "request/form";
    }

    @PostMapping("/customer/requests/{id}")
    public String update(Authentication authentication, @PathVariable Long id,
                         @Valid @ModelAttribute GuestRequestForm guestRequestForm,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareCustomerForm(authentication, model, guestRequestForm, id);
            return "request/form";
        }
        try {
            requestService.updateOwn(authentication, id, guestRequestForm);
        } catch (BusinessRuleException ex) {
            bindingResult.reject("request.invalid", ex.getMessage());
            prepareCustomerForm(authentication, model, guestRequestForm, id);
            return "request/form";
        }
        redirectAttributes.addFlashAttribute("success", "Request updated.");
        return "redirect:/customer/requests/" + id;
    }

    @PostMapping("/customer/requests/{id}/cancel")
    public String cancel(Authentication authentication, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        requestService.cancelOwn(authentication, id);
        redirectAttributes.addFlashAttribute("success", "Request cancelled.");
        return "redirect:/customer/requests/" + id;
    }

    @GetMapping("/staff/requests")
    public String manage(@RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) RequestStatus status,
                         @RequestParam(required = false) RequestPriority priority,
                         @RequestParam(required = false) RequestType type,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", requestService.search(q, status, priority, type, page));
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("priorities", RequestPriority.values());
        model.addAttribute("types", RequestType.values());
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedType", type);
        return "request/manage-list";
    }

    @GetMapping("/staff/requests/{id}")
    public String staffDetail(@PathVariable Long id, Model model) {
        prepareDetail(model, requestService.detailed(id), true);
        return "request/detail";
    }

    @PostMapping("/staff/requests/{id}/assign")
    public String assign(Authentication authentication, @PathVariable Long id,
                         @RequestParam Long staffId, RedirectAttributes redirectAttributes) {
        requestService.assign(authentication, id, staffId);
        redirectAttributes.addFlashAttribute("success", "Request assigned.");
        return "redirect:/staff/requests/" + id;
    }

    @PostMapping("/staff/requests/{id}/priority")
    public String priority(Authentication authentication, @PathVariable Long id,
                           @RequestParam RequestPriority priority, RedirectAttributes redirectAttributes) {
        requestService.updatePriority(authentication, id, priority);
        redirectAttributes.addFlashAttribute("success", "Priority updated.");
        return "redirect:/staff/requests/" + id;
    }

    @PostMapping("/staff/requests/{id}/responses")
    public String respond(Authentication authentication, @PathVariable Long id,
                          @Valid @ModelAttribute RequestResponseForm requestResponseForm,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareDetail(model, requestService.detailed(id), true);
            return "request/detail";
        }
        requestService.addResponse(authentication, id, requestResponseForm.getMessage());
        redirectAttributes.addFlashAttribute("success", "Response added.");
        return "redirect:/staff/requests/" + id;
    }

    @PostMapping("/staff/requests/{id}/start")
    public String start(Authentication authentication, @PathVariable Long id,
                        RedirectAttributes redirectAttributes) {
        requestService.start(authentication, id);
        redirectAttributes.addFlashAttribute("success", "Request moved to in progress.");
        return "redirect:/staff/requests/" + id;
    }

    @PostMapping("/staff/requests/{id}/resolve")
    public String resolve(Authentication authentication, @PathVariable Long id,
                          @RequestParam String resolution, RedirectAttributes redirectAttributes) {
        requestService.resolve(authentication, id, resolution);
        redirectAttributes.addFlashAttribute("success", "Request resolved.");
        return "redirect:/staff/requests/" + id;
    }

    @PostMapping("/staff/requests/{id}/close")
    public String close(Authentication authentication, @PathVariable Long id,
                        RedirectAttributes redirectAttributes) {
        requestService.close(authentication, id);
        redirectAttributes.addFlashAttribute("success", "Request closed.");
        return "redirect:/staff/requests/" + id;
    }

    @PostMapping("/staff/requests/{id}/reopen")
    public String reopen(Authentication authentication, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        requestService.reopen(authentication, id);
        redirectAttributes.addFlashAttribute("success", "Request reopened.");
        return "redirect:/staff/requests/" + id;
    }

    private void prepareCustomerForm(Authentication authentication, Model model,
                                     GuestRequestForm form, Long requestId) {
        model.addAttribute("guestRequestForm", form);
        model.addAttribute("requestId", requestId);
        model.addAttribute("types", RequestType.values());
        model.addAttribute("priorities", RequestPriority.values());
        model.addAttribute("reservations", reservationService.ownReservations(authentication, 0));
    }

    private void prepareDetail(Model model, GuestRequest request, boolean staffView) {
        model.addAttribute("guestRequest", request);
        model.addAttribute("responses", requestService.responses(request.getId()));
        model.addAttribute("history", requestService.history(request.getId()));
        model.addAttribute("staffView", staffView);
        if (staffView) {
            model.addAttribute("staff", requestService.activeStaff());
            model.addAttribute("priorities", RequestPriority.values());
            model.addAttribute("requestResponseForm", new RequestResponseForm());
        }
    }
}
