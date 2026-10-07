package com.staylanka.payment;
import com.staylanka.common.*;
import com.staylanka.reservation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@Transactional(readOnly=true)
public class PaymentService {
 private final PaymentRepository payments;
 private final PaymentReservationRepository locks;
 private final ReservationRepository reservations;
 public PaymentService(PaymentRepository payments,PaymentReservationRepository locks,ReservationRepository reservations){
  this.payments=payments;this.locks=locks;this.reservations=reservations;
 }
 public boolean isStaff(Authentication auth){
  return auth!=null && auth.getAuthorities().stream().anyMatch(a->Set.of("ROLE_ADMIN","ROLE_RESERVATION_MANAGER","ROLE_STAY_MANAGER").contains(a.getAuthority()));
 }
 public Reservation view(Long id,Authentication auth){
  var r=reservations.findDetailedById(id).orElseThrow(()->new NotFoundException("Reservation not found."));
  boolean customer=auth!=null && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_CUSTOMER"));
  if(!isStaff(auth) && !(customer && r.getCustomer().getUser().getEmail().equalsIgnoreCase(auth.getName())))
   throw new AccessDeniedException("You cannot access this payment account.");
  return r;
 }
 public List<PaymentEntry> entries(Long id,Authentication auth){view(id,auth);return payments.findByReservationIdOrderByCreatedAtDescIdDesc(id);}
 public PaymentSummary summary(Reservation r){return PaymentSummary.of(r.getTotalAmount(),payments.findByReservationIdOrderByCreatedAtDescIdDesc(r.getId()));}
 public PaymentEntry receipt(String reference,Authentication auth){
  var entry=payments.findByReference(reference).orElseThrow(()->new NotFoundException("Receipt not found."));
  view(entry.getReservation().getId(),auth);return entry;
 }
 @Transactional
 public PaymentEntry record(Long id,PaymentForm form,Authentication auth){
  if(!isStaff(auth)) throw new AccessDeniedException("Only authorised staff can record payments.");
  var r=locks.lockReservation(id).orElseThrow(()->new NotFoundException("Reservation not found."));
  var existing=payments.findByRequestKey(form.getRequestKey());
  if(existing.isPresent()) {
   var e=existing.get();
   if(!e.getReservation().getId().equals(id) || e.getKind()!=form.getKind() || e.getMethod()!=form.getMethod()
       || e.getAmount().compareTo(form.getAmount())!=0 || !Objects.equals(e.getExternalReference(),form.getExternalReference())
       || !Objects.equals(e.getNote(),form.getNote())) throw new BusinessRuleException("This submission key has already been used.");
   return e;
  }
  if(form.getAmount()==null || form.getAmount().signum()<=0 || form.getAmount().scale()>2
     || form.getAmount().compareTo(new java.math.BigDecimal("9999999999.99"))>0)
   throw new BusinessRuleException("Enter a valid positive LKR amount with at most two decimal places.");
  if(form.getKind()==null || form.getMethod()==null) throw new BusinessRuleException("Select a payment type and method.");
  var summary=summary(r);
  if(form.getKind()==PaymentKind.PAYMENT){
   if(!Set.of(ReservationStatus.CONFIRMED,ReservationStatus.CHECKED_IN,ReservationStatus.CHECKED_OUT).contains(r.getStatus()))
    throw new BusinessRuleException("Payments can only be recorded for confirmed or completed reservations.");
   if(form.getAmount().compareTo(summary.balance())>0) throw new BusinessRuleException("Payment exceeds the outstanding reservation balance.");
  } else {
   if(form.getAmount().compareTo(summary.netPaid())>0) throw new BusinessRuleException("Refund exceeds the net amount received.");
   if(form.getNote()==null || form.getNote().isBlank()) throw new BusinessRuleException("Enter a reason for the refund.");
  }
  if(form.getMethod()!=PaymentMethod.CASH && (form.getExternalReference()==null || form.getExternalReference().isBlank()))
   throw new BusinessRuleException("Enter the bank transfer or card-terminal reference.");
  return payments.saveAndFlush(new PaymentEntry(r,form,auth.getName()));
 }
}
