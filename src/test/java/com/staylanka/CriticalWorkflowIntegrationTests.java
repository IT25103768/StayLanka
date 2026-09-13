package com.staylanka;

import com.staylanka.auth.RegistrationForm;
import com.staylanka.auth.RegistrationService;
import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import com.staylanka.promotion.PromotionForm;
import com.staylanka.promotion.PromotionService;
import com.staylanka.promotion.PromotionType;
import com.staylanka.request.GuestRequest;
import com.staylanka.request.GuestRequestForm;
import com.staylanka.request.GuestRequestService;
import com.staylanka.request.RequestPriority;
import com.staylanka.request.RequestStatus;
import com.staylanka.request.RequestType;
import com.staylanka.reservation.Reservation;
import com.staylanka.reservation.ReservationForm;
import com.staylanka.reservation.ReservationService;
import com.staylanka.reservation.ReservationStatus;
import com.staylanka.review.Review;
import com.staylanka.review.ReviewForm;
import com.staylanka.review.ReviewService;
import com.staylanka.review.ReviewStatus;
import com.staylanka.room.Room;
import com.staylanka.room.RoomRepository;
import com.staylanka.room.RoomSearchForm;
import com.staylanka.room.RoomService;
import com.staylanka.room.RoomStatus;
import com.staylanka.room.RoomType;
import com.staylanka.room.RoomTypeRepository;
import com.staylanka.stay.AdditionalChargeForm;
import com.staylanka.stay.CheckInForm;
import com.staylanka.stay.CheckOutForm;
import com.staylanka.stay.Stay;
import com.staylanka.stay.StayService;
import com.staylanka.user.AppUser;
import com.staylanka.user.AppUserRepository;
import com.staylanka.user.Role;
import com.staylanka.user.StaffProfile;
import com.staylanka.user.StaffProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CriticalWorkflowIntegrationTests {

    @Autowired RegistrationService registrationService;
    @Autowired ReservationService reservationService;
    @Autowired PromotionService promotionService;
    @Autowired StayService stayService;
    @Autowired ReviewService reviewService;
    @Autowired GuestRequestService requestService;
    @Autowired RoomTypeRepository roomTypeRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired RoomService roomService;
    @Autowired AppUserRepository userRepository;
    @Autowired StaffProfileRepository staffProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void reservationSnapshotsPricePreventsOverlapAndCancellationReleasesDates() {
        Authentication customer = registerCustomer("booking.customer@staylanka.test");
        Room room = createRoom("T101", 3, "10000.00");
        ReservationForm form = reservationForm(room, LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(4), 2);

        Reservation first = reservationService.create(customer, form);

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(first.getNumberOfNights()).isEqualTo(3);
        assertThat(first.getNightlyPriceSnapshot()).isEqualByComparingTo("10000.00");
        assertThat(first.getGrossTotal()).isEqualByComparingTo("30000.00");
        assertThat(first.getTotalAmount()).isEqualByComparingTo("30000.00");
        assertThatThrownBy(() -> reservationService.create(customer, form))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlapping dates");

        reservationService.cancelOwn(customer, first.getId(), "Travel dates changed");
        Reservation replacement = reservationService.create(customer, form);

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(replacement.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void reservationRejectsPastDatesCapacityAndMaintenanceRooms() {
        Authentication customer = registerCustomer("validation.customer@staylanka.test");
        Room room = createRoom("T102", 2, "9000.00");

        ReservationForm past = reservationForm(room, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1);
        assertThatThrownBy(() -> reservationService.create(customer, past))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("past");

        ReservationForm overCapacity = reservationForm(room, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 3);
        assertThatThrownBy(() -> reservationService.create(customer, overCapacity))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("capacity");

        room.setStatus(RoomStatus.MAINTENANCE);
        ReservationForm maintenance = reservationForm(room, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 1);
        assertThatThrownBy(() -> reservationService.create(customer, maintenance))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("not available");
    }

    @Test
    void roomSearchExcludesOperationallyUnavailableAndOverlappingRooms() {
        Authentication customer = registerCustomer("availability.customer@staylanka.test");
        Room available = createRoom("T104", 2, "11000.00");
        Room maintenance = createRoom("T105", 4, "13000.00");
        maintenance.setStatus(RoomStatus.MAINTENANCE);

        RoomSearchForm search = new RoomSearchForm();
        search.setCheckIn(LocalDate.now().plusDays(1));
        search.setCheckOut(LocalDate.now().plusDays(3));
        search.setGuests(2);

        assertThat(roomService.available(search, 0).getContent())
                .contains(available)
                .doesNotContain(maintenance);
        assertThat(roomService.browsePublic("", 0).getContent()).doesNotContain(maintenance);

        Reservation reservation = reservationService.create(customer,
                reservationForm(available, search.getCheckIn(), search.getCheckOut(), 2));
        assertThat(roomService.available(search, 0).getContent()).doesNotContain(available);

        reservationService.cancelOwn(customer, reservation.getId(), "Plans changed");
        assertThat(roomService.available(search, 0).getContent()).contains(available);
    }

    @Test
    void percentageAndFixedPromotionStrategiesEnforceEligibilityAndZeroFloor() {
        PromotionForm percentage = promotionForm("TEST10", PromotionType.PERCENTAGE, "10.00");
        promotionService.create(percentage);
        PromotionService.PromotionResult tenPercent = promotionService.apply("test10", new BigDecimal("25000.00"), 3);
        assertThat(tenPercent.discount()).isEqualByComparingTo("2500.00");

        PromotionForm fixed = promotionForm("FIXED50K", PromotionType.FIXED_AMOUNT, "50000.00");
        fixed.setMinimumAmount(BigDecimal.ZERO);
        promotionService.create(fixed);
        PromotionService.PromotionResult floored = promotionService.apply("FIXED50K", new BigDecimal("12000.00"), 2);
        assertThat(floored.discount()).isEqualByComparingTo("12000.00");

        assertThatThrownBy(() -> promotionService.apply("TEST10", new BigDecimal("25000.00"), 1))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("at least");
    }

    @Test
    void completeGuestJourneyChecksInAddsChargesChecksOutAndAllowsOneReview() {
        Authentication customer = registerCustomer("journey.customer@staylanka.test");
        Room room = createRoom("T103", 4, "12000.00");
        Reservation reservation = reservationService.create(customer,
                reservationForm(room, LocalDate.now(), LocalDate.now().plusDays(2), 2));
        reservationService.confirm(reservation.getId());

        CheckInForm checkInForm = new CheckInForm();
        checkInForm.setActualCheckIn(LocalDateTime.now().minusHours(1));
        checkInForm.setGuestCount(2);
        Stay stay = stayService.checkIn(reservation.getId(), checkInForm);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);

        AdditionalChargeForm charge = new AdditionalChargeForm();
        charge.setDescription("Airport transfer");
        charge.setQuantity(2);
        charge.setUnitPrice(new BigDecimal("750.00"));
        stayService.addCharge(stay.getId(), charge);

        CheckOutForm checkOutForm = new CheckOutForm();
        checkOutForm.setActualCheckOut(LocalDateTime.now());
        stayService.checkOut(stay.getId(), checkOutForm);

        assertThat(stay.isCompleted()).isTrue();
        assertThat(stay.getAdditionalChargeTotal()).isEqualByComparingTo("1500.00");
        assertThat(stay.getFinalTotal()).isEqualByComparingTo("25500.00");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);

        ReviewForm reviewForm = new ReviewForm();
        reviewForm.setRating(5);
        reviewForm.setComment("Excellent service and a very comfortable stay.");
        Review review = reviewService.create(customer, stay.getId(), reviewForm);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThatThrownBy(() -> reviewService.create(customer, stay.getId(), reviewForm))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already");
    }

    @Test
    void requestWorkflowRecordsAssignmentResponsesTransitionsAndOwnership() {
        Authentication customer = registerCustomer("request.customer@staylanka.test");
        Authentication otherCustomer = registerCustomer("other.customer@staylanka.test");
        Authentication staff = createStaff("request.staff@staylanka.test");
        AppUser staffUser = userRepository.findByEmailIgnoreCase(staff.getName()).orElseThrow();

        GuestRequestForm form = new GuestRequestForm();
        form.setCategory("Arrival");
        form.setSubject("Late-night airport transfer");
        form.setDescription("Please arrange an airport transfer for a late-night arrival.");
        form.setType(RequestType.SPECIAL_REQUEST);
        form.setPriority(RequestPriority.HIGH);
        GuestRequest request = requestService.create(customer, form);

        assertThatThrownBy(() -> requestService.own(otherCustomer, request.getId()))
                .isInstanceOf(NotFoundException.class);

        requestService.assign(staff, request.getId(), staffUser.getId());
        requestService.start(staff, request.getId());
        requestService.addResponse(staff, request.getId(), "Transfer team has confirmed the pickup.");
        requestService.resolve(staff, request.getId(), "Driver and vehicle details sent to the guest.");
        requestService.close(staff, request.getId());

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(request.getAssignedStaff().getEmail()).isEqualTo(staff.getName());
        assertThat(requestService.responses(request.getId())).hasSize(1);
        assertThat(requestService.history(request.getId()))
                .extracting(item -> item.getNewStatus())
                .containsExactly(RequestStatus.SUBMITTED, RequestStatus.ASSIGNED,
                        RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED, RequestStatus.CLOSED);
    }

    private Authentication registerCustomer(String email) {
        RegistrationForm form = new RegistrationForm();
        form.setEmail(email);
        form.setPassword("StrongPass123");
        form.setConfirmPassword("StrongPass123");
        form.setFirstName("Test");
        form.setLastName("Customer");
        form.setPhone("+94771234567");
        registrationService.register(form);
        return authentication(email, Role.CUSTOMER);
    }

    private Authentication createStaff(String email) {
        AppUser user = userRepository.save(new AppUser(email, passwordEncoder.encode("StrongPass123"), Role.STAFF));
        staffProfileRepository.save(new StaffProfile(user, "Test", "Staff", "Front Desk"));
        return authentication(email, Role.STAFF);
    }

    private Authentication authentication(String email, Role role) {
        return new UsernamePasswordAuthenticationToken(email, "",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    private Room createRoom(String number, int capacity, String price) {
        RoomType type = roomTypeRepository.save(new RoomType("Type " + number, "Test room type", capacity,
                "One king bed", new BigDecimal(price), "Wi-Fi"));
        return roomRepository.save(new Room(number, type, "Integration-test room",
                new BigDecimal(price), RoomStatus.AVAILABLE));
    }

    private ReservationForm reservationForm(Room room, LocalDate checkIn, LocalDate checkOut, int guests) {
        ReservationForm form = new ReservationForm();
        form.setRoomId(room.getId());
        form.setCheckInDate(checkIn);
        form.setCheckOutDate(checkOut);
        form.setGuestCount(guests);
        return form;
    }

    private PromotionForm promotionForm(String code, PromotionType type, String value) {
        PromotionForm form = new PromotionForm();
        form.setCode(code);
        form.setName("Test promotion " + code);
        form.setDescription("Integration-test promotion");
        form.setType(type);
        form.setValue(new BigDecimal(value));
        form.setStartDate(LocalDate.now().minusDays(1));
        form.setEndDate(LocalDate.now().plusMonths(1));
        form.setMinimumNights(2);
        form.setMinimumAmount(new BigDecimal("10000.00"));
        return form;
    }
}
