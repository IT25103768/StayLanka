package com.staylanka.payment;
import com.staylanka.common.BusinessRuleException;
import com.staylanka.reservation.*;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.user.AppUser;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PaymentServiceTests {
 PaymentRepository payments; PaymentReservationRepository locks; ReservationRepository reservations;
 PaymentService service; Reservation reservation;
 final UsernamePasswordAuthenticationToken staff=new UsernamePasswordAuthenticationToken("staff@test", "", List.of(new SimpleGrantedAuthority("ROLE_STAY_MANAGER")));
 @BeforeEach void setup(){
  payments=mock(PaymentRepository.class);locks=mock(PaymentReservationRepository.class);reservations=mock(ReservationRepository.class);
  service=new PaymentService(payments,locks,reservations);reservation=mock(Reservation.class);
  when(reservation.getId()).thenReturn(1L);when(reservation.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
  when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
  when(locks.lockReservation(1L)).thenReturn(Optional.of(reservation));
  when(payments.findByRequestKey(anyString())).thenReturn(Optional.empty());
  when(payments.findByReservationIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of());
  when(payments.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
 }
 PaymentForm form(String amount){var f=new PaymentForm();f.setAmount(new BigDecimal(amount));return f;}
 PaymentEntry entry(PaymentKind kind,String amount){var e=mock(PaymentEntry.class);when(e.getKind()).thenReturn(kind);when(e.getAmount()).thenReturn(new BigDecimal(amount));return e;}
 @Test void partialPaymentsAndRefundsCalculateNetBalance(){
  var s=PaymentSummary.of(new BigDecimal("100"),List.of(entry(PaymentKind.PAYMENT,"80"),entry(PaymentKind.REFUND,"20")));
  assertEquals(0,s.netPaid().compareTo(new BigDecimal("60")));assertEquals(0,s.balance().compareTo(new BigDecimal("40")));assertEquals("PARTIALLY_PAID",s.status());
 }
 @Test void overpaymentIsRejected(){assertThrows(BusinessRuleException.class,()->service.record(1L,form("101"),staff));verify(payments,never()).saveAndFlush(any());}
 @Test void pendingReservationCannotTakePayment(){when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);assertThrows(BusinessRuleException.class,()->service.record(1L,form("10"),staff));}
 @Test void refundCannotExceedNetPaid(){var paidEntry=entry(PaymentKind.PAYMENT,"30");when(payments.findByReservationIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(paidEntry));var f=form("31");f.setKind(PaymentKind.REFUND);f.setNote("Cancellation");assertThrows(BusinessRuleException.class,()->service.record(1L,f,staff));}
 @Test void refundRequiresReason(){var paidEntry=entry(PaymentKind.PAYMENT,"30");when(payments.findByReservationIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(paidEntry));var f=form("10");f.setKind(PaymentKind.REFUND);assertThrows(BusinessRuleException.class,()->service.record(1L,f,staff));}
 @Test void terminalPaymentRequiresReference(){var f=form("10");f.setMethod(PaymentMethod.CARD_TERMINAL);assertThrows(BusinessRuleException.class,()->service.record(1L,f,staff));}
 @Test void duplicateSubmissionDoesNotSaveTwice(){var f=form("10");var existing=new PaymentEntry(reservation,f,staff.getName());when(payments.findByRequestKey(f.getRequestKey())).thenReturn(Optional.of(existing));assertSame(existing,service.record(1L,f,staff));verify(payments,never()).saveAndFlush(any());}
 @Test void customerCannotRecordPayment(){var customer=new UsernamePasswordAuthenticationToken("guest@test","",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));assertThrows(AccessDeniedException.class,()->service.record(1L,form("10"),customer));verifyNoInteractions(locks);}
 @Test void customerCannotReadAnotherGuestsAccount(){
  var profile=mock(CustomerProfile.class);var user=mock(AppUser.class);when(user.getEmail()).thenReturn("owner@test");when(profile.getUser()).thenReturn(user);when(reservation.getCustomer()).thenReturn(profile);when(reservations.findDetailedById(1L)).thenReturn(Optional.of(reservation));
  var customer=new UsernamePasswordAuthenticationToken("other@test","",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));assertThrows(AccessDeniedException.class,()->service.view(1L,customer));
 }
 @Test void actualPaymentCanBeRecorded(){assertEquals(0,service.record(1L,form("40"),staff).getAmount().compareTo(new BigDecimal("40")));verify(payments).saveAndFlush(any());}
}
