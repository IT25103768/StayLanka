```java
        package com.staylanka.stay;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.NotFoundException;
import com.staylanka.reservation.Reservation;
import com.staylanka.reservation.ReservationRepository;
import com.staylanka.reservation.ReservationService;
import com.staylanka.reservation.ReservationStatus;
import com.staylanka.room.Room;
import com.staylanka.room.RoomRepository;
import com.staylanka.room.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StayService {

    private static final int PAGE_SIZE = 12;

    private final StayRepository stayRepository;
    private final AdditionalChargeRepository chargeRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public StayService(
            StayRepository stayRepository,
            AdditionalChargeRepository chargeRepository,
            ReservationRepository reservationRepository,
            ReservationService reservationService,
            RoomRepository roomRepository) {

        this.stayRepository = stayRepository;
        this.chargeRepository = chargeRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    // -------------------------------------------------------------------------
    // CHECK-IN
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Reservation> eligibleForCheckIn(int page) {
        LocalDate today = LocalDate.now();

        PageRequest pageable = PageRequest.of(
                normalizePage(page),
                PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "checkInDate")
        );

        return reservationRepository
                .findByStatusAndCheckInDateLessThanEqualAndCheckOutDateAfter(
                        ReservationStatus.CONFIRMED,
                        today,
                        today,
                        pageable
                );
    }

    @Transactional(readOnly = true)
    public Reservation eligibleReservation(Long reservationId) {
        validateId(reservationId, "Reservation ID");

        Reservation reservation = reservationService.detailed(reservationId);

        if (!isEligibleForCheckIn(reservation)) {
            throw new BusinessRuleException(
                    "This reservation is not eligible for check-in."
            );
        }

        return reservation;
    }

    @Transactional
    public Stay checkIn(Long reservationId, CheckInForm form) {
        validateId(reservationId, "Reservation ID");

        if (form == null) {
            throw new BusinessRuleException("Check-in details are required.");
        }

        Reservation reservation = reservationService.detailed(reservationId);

        validateCheckInReservation(reservation, reservationId);
        validateCheckInForm(form, reservation);

        Room room = roomRepository.findByIdForUpdate(reservation.getRoom().getId())
                .orElseThrow(() ->
                        new NotFoundException("Assigned room was not found."));

        validateRoomForCheckIn(room, form.getGuestCount());

        Stay stay = new Stay(
                reservation,
                room,
                form.getActualCheckIn(),
                form.getGuestCount(),
                trimToNull(form.getNotes()),
                reservation.getTotalAmount()
        );

        Stay savedStay = stayRepository.save(stay);

        room.setStatus(RoomStatus.OCCUPIED);
        reservationService.markCheckedIn(reservation);

        return savedStay;
    }

    // -------------------------------------------------------------------------
    // STAY RETRIEVAL
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Stay get(Long id) {
        validateId(id, "Stay ID");

        return stayRepository.findDetailedById(id)
                .orElseThrow(() ->
                        new NotFoundException("Stay was not found."));
    }

    @Transactional(readOnly = true)
    public Stay own(Authentication authentication, Long id) {
        validateAuthentication(authentication);
        validateId(id, "Stay ID");

        Stay stay = get(id);

        String userEmail = authentication.getName();

        if (stay.getReservation()
                .getCustomer()
                .getUser()
                .getEmail() == null
                || !stay.getReservation()
                .getCustomer()
                .getUser()
                .getEmail()
                .equalsIgnoreCase(userEmail)) {

            throw new NotFoundException("Stay was not found.");
        }

        return stay;
    }

    @Transactional(readOnly = true)
    public Page<Stay> ownHistory(
            Authentication authentication,
            int page) {

        validateAuthentication(authentication);

        PageRequest pageable = PageRequest.of(
                normalizePage(page),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "actualCheckIn")
        );

        return stayRepository.findByReservationCustomerUserEmailIgnoreCase(
                authentication.getName(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Stay> operational(boolean completed, int page) {
        PageRequest pageable = PageRequest.of(
                normalizePage(page),
                PAGE_SIZE,
                Sort.by(
                        Sort.Direction.DESC,
                        completed ? "actualCheckOut" : "actualCheckIn"
                )
        );

        return completed
                ? stayRepository.findByActualCheckOutIsNotNull(pageable)
                : stayRepository.findByActualCheckOutIsNull(pageable);
    }

    // -------------------------------------------------------------------------
    // ADDITIONAL CHARGES
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdditionalCharge> charges(Long stayId) {
        validateId(stayId, "Stay ID");

        return chargeRepository.findByStayIdOrderByCreatedAtAsc(stayId);
    }

    @Transactional
    public void addCharge(
            Long stayId,
            AdditionalChargeForm form) {

        validateId(stayId, "Stay ID");

        if (form == null) {
            throw new BusinessRuleException(
                    "Additional charge details are required."
            );
        }

        Stay stay = openStay(stayId);

        AdditionalCharge charge = new AdditionalCharge(
                stay,
                form.getDescription().trim(),
                form.getQuantity(),
                form.getUnitPrice()
        );

        chargeRepository.save(charge);
        chargeRepository.flush();

        recalculate(stay);
    }

    @Transactional(readOnly = true)
    public AdditionalCharge getCharge(
            Long stayId,
            Long chargeId) {

        validateId(stayId, "Stay ID");
        validateId(chargeId, "Charge ID");

        AdditionalCharge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Additional charge was not found."
                        ));

        if (charge.getStay() == null
                || charge.getStay().getId() == null
                || !charge.getStay().getId().equals(stayId)) {

            throw new NotFoundException(
                    "Additional charge was not found."
            );
        }

        return charge;
    }

    @Transactional
    public void updateCharge(
            Long stayId,
            Long chargeId,
            AdditionalChargeForm form) {

        validateId(stayId, "Stay ID");
        validateId(chargeId, "Charge ID");

        if (form == null) {
            throw new BusinessRuleException(
                    "Additional charge details are required."
            );
        }

        Stay stay = openStay(stayId);
        AdditionalCharge charge = getCharge(stayId, chargeId);

        charge.update(
                form.getDescription().trim(),
                form.getQuantity(),
                form.getUnitPrice()
        );

        chargeRepository.flush();
        recalculate(stay);
    }

    @Transactional
    public void deleteCharge(Long stayId, Long chargeId) {
        validateId(stayId, "Stay ID");
        validateId(chargeId, "Charge ID");

        Stay stay = openStay(stayId);
        AdditionalCharge charge = getCharge(stayId, chargeId);

        chargeRepository.delete(charge);
        chargeRepository.flush();

        recalculate(stay);
    }

    // -------------------------------------------------------------------------
    // CHECK-OUT
    // -------------------------------------------------------------------------

    @Transactional
    public void checkOut(Long stayId, CheckOutForm form) {
        validateId(stayId, "Stay ID");

        if (form == null) {
            throw new BusinessRuleException(
                    "Check-out details are required."
            );
        }

        Stay stay = stayRepository.findDetailedByIdForUpdate(stayId)
                .orElseThrow(() ->
                        new NotFoundException("Stay was not found."));

        if (stay.isCompleted()) {
            throw new BusinessRuleException(
                    "This stay has already been checked out."
            );
        }

        validateCheckOutTime(
                form.getActualCheckOut(),
                stay.getActualCheckIn()
        );

        Room room = roomRepository.findByIdForUpdate(stay.getRoom().getId())
                .orElseThrow(() ->
                        new NotFoundException("Assigned room was not found."));

        BigDecimal additionalTotal =
                chargeRepository.totalForStay(stayId);

        if (additionalTotal == null) {
            additionalTotal = BigDecimal.ZERO;
        }

        stay.checkOut(
                form.getActualCheckOut(),
                additionalTotal
        );

        room.setStatus(RoomStatus.AVAILABLE);

        reservationService.markCheckedOut(
                stay.getReservation()
        );
    }

    // -------------------------------------------------------------------------
    // DASHBOARD
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public long currentCount() {
        return stayRepository.countByActualCheckOutIsNull();
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private boolean isEligibleForCheckIn(Reservation reservation) {
        if (reservation == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        return reservation.getStatus() == ReservationStatus.CONFIRMED
                && !today.isBefore(reservation.getCheckInDate())
                && today.isBefore(reservation.getCheckOutDate())
                && reservation.getRoom() != null
                && reservation.getRoom().getId() != null
                && reservation.getId() != null
                && !stayRepository.existsByReservationId(
                reservation.getId()
        );
    }

    private void validateCheckInReservation(
            Reservation reservation,
            Long reservationId) {

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "Only a confirmed reservation can be checked in."
            );
        }

        if (stayRepository.existsByReservationId(reservationId)) {
            throw new BusinessRuleException(
                    "This reservation has already been checked in."
            );
        }

        LocalDate today = LocalDate.now();

        if (today.isBefore(reservation.getCheckInDate())) {
            throw new BusinessRuleException(
                    "This reservation is not yet eligible for check-in."
            );
        }

        if (!today.isBefore(reservation.getCheckOutDate())) {
            throw new BusinessRuleException(
                    "The reservation's check-out date has passed."
            );
        }

        if (reservation.getRoom() == null
                || reservation.getRoom().getId() == null) {

            throw new BusinessRuleException(
                    "No valid room is assigned to this reservation."
            );
        }
    }

    private void validateCheckInForm(
            CheckInForm form,
            Reservation reservation) {

        LocalDateTime actualCheckIn = form.getActualCheckIn();

        if (actualCheckIn == null) {
            throw new BusinessRuleException(
                    "Actual check-in time is required."
            );
        }

        if (actualCheckIn.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Actual check-in time cannot be in the future."
            );
        }

        if (actualCheckIn.toLocalDate()
                .isBefore(reservation.getCheckInDate())
                || !actualCheckIn.toLocalDate()
                .isBefore(reservation.getCheckOutDate())) {

            throw new BusinessRuleException(
                    "Actual check-in time must fall within the reservation dates."
            );
        }

        if (form.getGuestCount() < 1) {
            throw new BusinessRuleException(
                    "Guest count must be at least 1."
            );
        }
    }

    private void validateRoomForCheckIn(
            Room room,
            int guestCount) {

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new BusinessRuleException(
                    "The assigned room is not operationally available."
            );
        }

        if (room.getRoomType() == null) {
            throw new BusinessRuleException(
                    "The assigned room has no valid room type."
            );
        }

        if (guestCount > room.getRoomType().getCapacity()) {
            throw new BusinessRuleException(
                    "Guest count exceeds the assigned room's capacity."
            );
        }
    }

    private void validateCheckOutTime(
            LocalDateTime actualCheckOut,
            LocalDateTime actualCheckIn) {

        if (actualCheckOut == null) {
            throw new BusinessRuleException(
                    "Actual check-out time is required."
            );
        }

        if (actualCheckOut.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Actual check-out time cannot be in the future."
            );
        }

        if (actualCheckIn == null
                || !actualCheckOut.isAfter(actualCheckIn)) {

            throw new BusinessRuleException(
                    "Check-out time must be after check-in time."
            );
        }
    }

    private Stay openStay(Long stayId) {
        Stay stay = stayRepository.findDetailedByIdForUpdate(stayId)
                .orElseThrow(() ->
                        new NotFoundException("Stay was not found."));

        if (stay.isCompleted()) {
            throw new BusinessRuleException(
                    "Charges cannot be changed after check-out."
            );
        }

        return stay;
    }

    private void recalculate(Stay stay) {
        BigDecimal total = chargeRepository.totalForStay(stay.getId());

        if (total == null) {
            total = BigDecimal.ZERO;
        }

        stay.updateAdditionalTotal(total);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a valid positive number."
            );
        }
    }

    private void validateAuthentication(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new NotFoundException("User was not found.");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
```
