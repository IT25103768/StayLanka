package com.staylanka.payment;
import com.staylanka.common.BusinessRuleException;
import com.staylanka.reservation.ReservationRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/payments")
public class PaymentController {
 private final PaymentService service;private final ReservationRepository reservations;
 public PaymentController(PaymentService service,ReservationRepository reservations){this.service=service;this.reservations=reservations;}
 @GetMapping
 @PreAuthorize("hasAnyRole('ADMIN','RESERVATION_MANAGER','STAY_MANAGER')")
 public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
  model.addAttribute("reservations",reservations.search(q,null,null,null,PageRequest.of(Math.max(0,page),12,org.springframework.data.domain.Sort.by("id").descending())));
  model.addAttribute("q",q);return "payment/list";
 }
 @GetMapping("/reservations/{id}")
 public String account(@PathVariable Long id,Authentication auth,Model model){
  model.addAttribute("paymentForm",new PaymentForm());prepare(id,auth,model);return "payment/account";
 }
 private void prepare(Long id,Authentication auth,Model model){
  var r=service.view(id,auth);model.addAttribute("reservation",r);model.addAttribute("summary",service.summary(r));
  model.addAttribute("entries",service.entries(id,auth));model.addAttribute("canRecord",service.isStaff(auth));
  model.addAttribute("methods",PaymentMethod.values());model.addAttribute("kinds",PaymentKind.values());
 }
 @PostMapping("/reservations/{id}")
 @PreAuthorize("hasAnyRole('ADMIN','RESERVATION_MANAGER','STAY_MANAGER')")
 public String record(@PathVariable Long id,@Valid @ModelAttribute PaymentForm paymentForm,BindingResult result,
                     Authentication auth,Model model,RedirectAttributes redirect){
  if(!result.hasErrors()) {
   try {var entry=service.record(id,paymentForm,auth);redirect.addFlashAttribute("success","Transaction recorded: "+entry.getReference());return "redirect:/payments/reservations/"+id;}
   catch(BusinessRuleException e){result.reject("payment.invalid",e.getMessage());}
  }
  prepare(id,auth,model);return "payment/account";
 }
 @GetMapping("/receipts/{reference}")
 public String receipt(@PathVariable String reference,Authentication auth,Model model){
  model.addAttribute("entry",service.receipt(reference,auth));return "payment/receipt";
 }
}
