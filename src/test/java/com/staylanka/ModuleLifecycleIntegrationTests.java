package com.staylanka;

import com.staylanka.auth.*;
import com.staylanka.common.*;
import com.staylanka.customer.*;
import com.staylanka.promotion.*;
import com.staylanka.request.*;
import com.staylanka.reservation.*;
import com.staylanka.review.*;
import com.staylanka.room.*;
import com.staylanka.stay.*;
import com.staylanka.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username="purge-admin",roles="ADMIN")
class ModuleLifecycleIntegrationTests {
    @Autowired EntityManager em;
    @Autowired PurgeService purge;
    @Autowired RegistrationService registration;
    @Autowired RoomService rooms;
    @Autowired RoomRepository roomRepository;
    @Autowired RoomTypeRepository types;
    @Autowired ReservationService reservations;
    @Autowired ReservationRepository reservationRepository;
    @Autowired CustomerService customers;
    @Autowired CustomerProfileRepository customerRepository;
    @Autowired ProfileOperationsService profiles;
    @Autowired StayService stays;
    @Autowired StayRepository stayRepository;
    @Autowired PromotionService promotions;
    @Autowired PromotionRepository promotionRepository;
    @Autowired ReviewService reviews;
    @Autowired ReviewRepository reviewRepository;
    @Autowired GuestRequestService requests;
    @Autowired GuestRequestRepository requestRepository;
    @Autowired AppUserRepository users;
    @Autowired RecoveryService recovery;
    @Autowired MaintenanceService maintenance;

    @Test void roomCreateReadUpdateDeactivateAndPurge() {
        Room room=room(); Long id=room.getId(); clear();
        assertThat(roomRepository.existsById(id)).isTrue();
        RoomForm form=RoomForm.from(rooms.get(id));form.setDescription("Updated facilities");rooms.update(id,form);clear();
        assertThat(rooms.get(id).getDescription()).isEqualTo("Updated facilities");
        rooms.deactivate(id);clear();assertThat(rooms.get(id).getStatus()).isEqualTo(RoomStatus.INACTIVE);
        purge.delete("Room",id,"DELETE Room "+id);clear();assertThat(roomRepository.existsById(id)).isFalse();auditExists("Room",id);
    }
    @Test void reservationCreateReadUpdateCancelAndPurge() {
        Authentication guest=guest();Room room=room();ReservationForm form=booking(room,LocalDate.now().plusDays(2));
        Reservation reservation=reservations.create(guest,form);Long id=reservation.getId();clear();
        assertThat(reservations.own(guest,id).getGuestCount()).isEqualTo(1);
        reservations.confirm(id);form.setGuestCount(2);reservations.updateOwn(guest,id,form);clear();
        assertThat(reservations.own(guest,id).getGuestCount()).isEqualTo(2);
        reservations.cancelOwn(guest,id,"Travel changed");clear();assertThat(reservations.detailed(id).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        purge.delete("Reservation",id,"DELETE Reservation "+id);clear();assertThat(reservationRepository.existsById(id)).isFalse();auditExists("Reservation",id);
    }
    @Test void profileCreateReadUpdateAnonymiseAndPurge() {
        WalkInForm form=new WalkInForm();form.setEmail(UUID.randomUUID()+"@test.lk");form.setFirstName("Walk");form.setLastName("In");
        CustomerProfile profile=profiles.create(form);Long id=profile.getId();Long userId=profile.getUser().getId();clear();
        assertThat(customers.get(id).getFirstName()).isEqualTo("Walk");
        CustomerProfileForm edit=CustomerProfileForm.from(customers.get(id));edit.setPreferences("Quiet room");edit.setMarketingConsent(true);profiles.update(id,edit);clear();
        assertThat(customers.get(id).getPreferences()).isEqualTo("Quiet room");
        profiles.anonymise(id);clear();assertThat(customers.get(id).getUser().isActive()).isFalse();assertThat(customers.get(id).getPreferences()).isNull();
        purge.delete("CustomerProfile",id,"DELETE CustomerProfile "+id);clear();assertThat(customerRepository.existsById(id)).isFalse();assertThat(users.existsById(userId)).isFalse();auditExists("CustomerProfile",id);
    }
    @Test void stayCreateReadMoveExtendVoidAndPurge() {
        Authentication guest=guest();Room first=room();Room second=room();Reservation booking=reservations.create(guest,booking(first,LocalDate.now()));reservations.confirm(booking.getId());
        Stay stay=stays.checkIn(booking.getId(),checkIn());Long id=stay.getId();clear();
        assertThat(stays.get(id).isIdentityVerified()).isTrue();
        stays.move(id,second.getId());stays.extend(id,LocalDate.now().plusDays(4));clear();
        assertThat(stays.get(id).getRoom().getId()).isEqualTo(second.getId());assertThat(rooms.get(first.getId()).getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(stays.get(id).getReservation().getCheckOutDate()).isEqualTo(LocalDate.now().plusDays(4));
        stays.voidStay(id);clear();assertThat(stays.get(id).isVoided()).isTrue();assertThat(stays.get(id).isCompleted()).isFalse();assertThat(rooms.get(second.getId()).getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        purge.delete("Stay",id,"DELETE Stay "+id);clear();assertThat(stayRepository.existsById(id)).isFalse();auditExists("Stay",id);
    }
    @Test void promotionCreateReadUpdateDeactivateAndPurge() {
        PromotionForm form=new PromotionForm();form.setCode("PROMO-"+UUID.randomUUID().toString().substring(0,8));form.setName("Offer");form.setType(PromotionType.PERCENTAGE);form.setValue(BigDecimal.TEN);form.setStartDate(LocalDate.now());form.setEndDate(LocalDate.now().plusDays(10));
        Long id=promotions.create(form).getId();clear();assertThat(promotions.get(id).getName()).isEqualTo("Offer");
        form.setName("Revised offer");promotions.update(id,form);clear();assertThat(promotions.get(id).getName()).isEqualTo("Revised offer");
        promotions.toggle(id);clear();assertThat(promotions.get(id).isActive()).isFalse();
        purge.delete("Promotion",id,"DELETE Promotion "+id);clear();assertThat(promotionRepository.existsById(id)).isFalse();auditExists("Promotion",id);
    }
    @Test void verifiedReviewCreateReadUpdateHideAndPurge() {
        Authentication guest=guest();Reservation booking=reservations.create(guest,booking(room(),LocalDate.now()));reservations.confirm(booking.getId());Stay stay=stays.checkIn(booking.getId(),checkIn());
        CheckOutForm checkout=new CheckOutForm();checkout.setActualCheckOut(LocalDateTime.now());stays.checkOut(stay.getId(),checkout);
        ReviewForm form=new ReviewForm();form.setRating(4);form.setComment("Comfortable room and kind service.");Long id=reviews.create(guest,stay.getId(),form).getId();clear();assertThat(reviews.detailed(id).getRating()).isEqualTo(4);
        form.setRating(5);reviews.update(guest,id,form);clear();assertThat(reviews.detailed(id).getRating()).isEqualTo(5);
        reviews.approve(id);reviews.delete(guest,id);clear();assertThat(reviews.detailed(id).getStatus()).isEqualTo(ReviewStatus.REJECTED);assertThat(reviewRepository.existsById(id)).isTrue();
        purge.delete("Review",id,"DELETE Review "+id);clear();assertThat(reviewRepository.existsById(id)).isFalse();auditExists("Review",id);
    }
    @Test void requestCreateReadUpdateCancelArchiveAndPurge() {
        Authentication guest=guest();GuestRequestForm form=new GuestRequestForm();form.setCategory("Arrival");form.setSubject("Transfer");form.setDescription("Please arrange an airport transfer.");form.setType(RequestType.SPECIAL_REQUEST);form.setPriority(RequestPriority.MEDIUM);
        Long id=requests.create(guest,form).getId();clear();assertThat(requests.own(guest,id).getOwnerLabel()).isEqualTo("Customer Relations queue");
        form.setSubject("Later transfer");requests.updateOwn(guest,id,form);clear();assertThat(requests.own(guest,id).getSubject()).isEqualTo("Later transfer");
        requests.cancelOwn(guest,id);requests.archive(guest,id);clear();assertThat(requests.detailed(id).isArchived()).isTrue();
        purge.delete("GuestRequest",id,"DELETE GuestRequest "+id);clear();assertThat(requestRepository.existsById(id)).isFalse();auditExists("GuestRequest",id);
    }
    @Test void purgeRefusesLinkedHistory() {
        Authentication guest=guest();Room room=room();Reservation booking=reservations.create(guest,booking(room,LocalDate.now().plusDays(2)));reservations.cancelOwn(guest,booking.getId(),"Cancelled");rooms.deactivate(room.getId());
        assertThatThrownBy(()->purge.delete("Room",room.getId(),"DELETE Room "+room.getId())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("linked Reservation");
        assertThat(roomRepository.existsById(room.getId())).isTrue();
    }
    @Test void checkInRequiresVerifiedIdentity() {
        Authentication guest=guest();Reservation booking=reservations.create(guest,booking(room(),LocalDate.now()));reservations.confirm(booking.getId());CheckInForm form=checkIn();form.setIdentityVerified(false);
        assertThatThrownBy(()->stays.checkIn(booking.getId(),form)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("identity");
    }
    @Test void scheduledMaintenanceBlocksSearchAndReservation() {
        Authentication guest=guest();Room room=room();LocalDate start=LocalDate.now().plusDays(3);
        maintenance.create(room.getId(),start,start.plusDays(2),"Bathroom repairs");
        RoomSearchForm search=new RoomSearchForm();search.setCheckIn(start);search.setCheckOut(start.plusDays(1));search.setGuests(1);
        assertThat(rooms.available(search,0).getContent()).extracting(Room::getId).doesNotContain(room.getId());
        assertThatThrownBy(()->reservations.create(guest,booking(room,start))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("maintenance");
    }
    @Test void recoveryTokenIsHashedExpiringAndSingleUse() {
        Authentication guest=guest();AppUser user=users.findByEmailIgnoreCase(guest.getName()).orElseThrow();recovery.request(user.getEmail());String token=recovery.issue(user.getId(),true);
        Object stored=em.createNativeQuery("SELECT token_hash FROM password_recovery WHERE user_id=?1").setParameter(1,user.getId()).getSingleResult();assertThat(stored.toString()).isNotEqualTo(token).hasSize(64);
        recovery.reset(token,"ReplacementPass123","ReplacementPass123");clear();
        assertThatThrownBy(()->recovery.reset(token,"OtherPassword123","OtherPassword123")).isInstanceOf(BusinessRuleException.class);
    }
    @Test @WithMockUser(roles="ROOM_MANAGER") void moduleManagerCannotPurge() {
        assertThatThrownBy(()->purge.delete("Room",1L,"DELETE Room 1")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    private void clear(){em.flush();em.clear();}
    private void auditExists(String type,Long id){Number count=(Number)em.createNativeQuery("SELECT COUNT(*) FROM audit_events WHERE entity_type=?1 AND entity_id=?2 AND action='HARD_DELETE'").setParameter(1,type).setParameter(2,id).getSingleResult();assertThat(count.longValue()).isEqualTo(1);}
    private Authentication guest(){RegistrationForm f=new RegistrationForm();f.setEmail(UUID.randomUUID()+"@test.lk");f.setFirstName("Test");f.setLastName("Guest");f.setPassword("StrongPass123");f.setConfirmPassword("StrongPass123");registration.register(f);return new UsernamePasswordAuthenticationToken(f.getEmail(),"",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));}
    private Room room(){String number=UUID.randomUUID().toString().substring(0,8);RoomType type=types.save(new RoomType("Type-"+number,"Garden",3,"King",new BigDecimal("10000"),"Wi-Fi"));RoomForm f=new RoomForm();f.setRoomNumber(number);f.setRoomTypeId(type.getId());f.setNightlyPrice(new BigDecimal("10000"));return rooms.create(f);}
    private ReservationForm booking(Room room,LocalDate start){ReservationForm f=new ReservationForm();f.setRoomId(room.getId());f.setCheckInDate(start);f.setCheckOutDate(start.plusDays(2));f.setGuestCount(1);return f;}
    private CheckInForm checkIn(){CheckInForm f=new CheckInForm();f.setActualCheckIn(LocalDate.now().atStartOfDay());f.setGuestCount(1);f.setIdentityVerified(true);return f;}
}
