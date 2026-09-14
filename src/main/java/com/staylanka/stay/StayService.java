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
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.room.MaintenanceService maintenance;
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.NotificationService notices;
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.InputRules rules;
    private final StayRepository stayRepository;
    private final AdditionalChargeRepository chargeRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public StayService(StayRepository stayRepository, AdditionalChargeRepository chargeRepository,
                       ReservationRepository reservationRepository, ReservationService reservationService,
                       RoomRepository roomRepository) {
        this.stayRepository = stayRepository;
        this.chargeRepository = chargeRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public Page<Reservation> eligibleForCheckIn(int page) {
        LocalDate today = LocalDate.now();
        return reservationRepository.findByStatusAndCheckInDateLessThanEqualAndCheckOutDateAfter(
                ReservationStatus.CONFIRMED, today, today,
                PageRequest.of(Math.max(page, 0), 12, Sort.by("checkInDate").ascending()));
    }

    @Transactional(readOnly = true)
    public Reservation eligibleReservation(Long reservationId) {
        Reservation reservation = reservationService.detailed(reservationId);
        LocalDate today = LocalDate.now();
        if (reservation.getStatus() != ReservationStatus.CONFIRMED
                || today.isBefore(reservation.getCheckInDate())
                || !today.isBefore(reservation.getCheckOutDate())
                || stayRepository.existsByReservationId(reservationId)) {
            throw new BusinessRuleException("This reservation is not eligible for check-in.");
        }
        return reservation;
    }

    @Transactional
    public Stay checkIn(Long reservationId, CheckInForm form) {
        rules.validate(form);
        Reservation reservation = reservationService.detailed(reservationId);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Only a confirmed reservation can be checked in.");
        }
        if (stayRepository.existsByReservationId(reservationId)) {
            throw new BusinessRuleException("This reservation has already been checked in.");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(reservation.getCheckInDate())) {
            throw new BusinessRuleException("This reservation is not yet eligible for check-in.");
        }
        if (!today.isBefore(reservation.getCheckOutDate())) {
            throw new BusinessRuleException("The reservation's check-out date has passed.");
        }
        if (form.getActualCheckIn() == null) {
            throw new BusinessRuleException("Actual check-in time is required.");
        }
        if (form.getActualCheckIn().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Actual check-in time cannot be in the future.");
        }
        LocalDate actualCheckInDate = form.getActualCheckIn().toLocalDate();
        if (actualCheckInDate.isBefore(reservation.getCheckInDate())
                || !actualCheckInDate.isBefore(reservation.getCheckOutDate())) {
            throw new BusinessRuleException("Actual check-in time must fall within the reservation dates.");
        }
        Room room = roomRepository.findByIdForUpdate(reservation.getRoom().getId())
                .orElseThrow(() -> new NotFoundException("Assigned room was not found."));
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new BusinessRuleException("The assigned room is not operationally available.");
        }
        if (form.getGuestCount() > room.getRoomType().getCapacity()) {
            throw new BusinessRuleException("Guest count exceeds the assigned room's capacity.");
        }
        Stay stay = stayRepository.save(new Stay(reservation, room, form.getActualCheckIn(),
                form.getGuestCount(), trimToNull(form.getNotes()), reservation.getTotalAmount()));
        stay.verifyIdentity();
        audit.record(stay, "CHECK_IN");
        room.setStatus(RoomStatus.OCCUPIED);
        reservationService.markCheckedIn(reservation);
        return stay;
    }

    @Transactional(readOnly = true)
    public Stay get(Long id) {
        return stayRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Stay was not found."));
    }

    @Transactional(readOnly = true)
    public Stay own(Authentication authentication, Long id) {
        Stay stay = get(id);
        if (!stay.getReservation().getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new NotFoundException("Stay was not found.");
        }
        return stay;
    }

    @Transactional(readOnly = true)
    public Page<Stay> ownHistory(Authentication authentication, int page) {
        return stayRepository.findByReservationCustomerUserEmailIgnoreCase(authentication.getName(),
                PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "actualCheckIn")));
    }

    @Transactional(readOnly = true)
    public Page<Stay> operational(boolean completed, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 12,
                Sort.by(Sort.Direction.DESC, completed ? "actualCheckOut" : "actualCheckIn"));
        return completed ? stayRepository.findByActualCheckOutIsNotNull(pageable)
                : stayRepository.findByActualCheckOutIsNullAndVoidedFalse(pageable);
    }

    @Transactional(readOnly = true)
    public List<AdditionalCharge> charges(Long stayId) {
        return chargeRepository.findByStayIdOrderByCreatedAtAsc(stayId);
    }

    @Transactional
    public void addCharge(Long stayId, AdditionalChargeForm form) {
        rules.validate(form);
        Stay stay = openStay(stayId);
        chargeRepository.save(new AdditionalCharge(stay, form.getDescription().trim(),
                form.getQuantity(), form.getUnitPrice()));
        chargeRepository.flush();
        recalculate(stay);
        audit.record(stay, "UPDATE_CHARGES");
    }

    @Transactional(readOnly = true)
    public AdditionalCharge getCharge(Long stayId, Long chargeId) {
        AdditionalCharge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Additional charge was not found."));
        if (!charge.getStay().getId().equals(stayId)) {
            throw new NotFoundException("Additional charge was not found.");
        }
        return charge;
    }

    @Transactional
    public void updateCharge(Long stayId, Long chargeId, AdditionalChargeForm form) {
        rules.validate(form);
        Stay stay = openStay(stayId);
        AdditionalCharge charge = getCharge(stayId, chargeId);
        charge.update(form.getDescription().trim(), form.getQuantity(), form.getUnitPrice());
        chargeRepository.flush();
        recalculate(stay);
        audit.record(stay, "UPDATE_CHARGES");
    }

    @Transactional
    public void deleteCharge(Long stayId, Long chargeId) {
        Stay stay = openStay(stayId);
        AdditionalCharge charge = getCharge(stayId, chargeId);
        chargeRepository.delete(charge);
        chargeRepository.flush();
        recalculate(stay);
        audit.record(stay, "UPDATE_CHARGES");
    }

    @Transactional
    public void checkOut(Long stayId, CheckOutForm form) {
        Stay stay = stayRepository.findDetailedByIdForUpdate(stayId)
                .orElseThrow(() -> new NotFoundException("Stay was not found."));
        if (stay.isCompleted() || stay.isVoided()) {
            throw new BusinessRuleException("This stay has already been checked out.");
        }
        if (form.getActualCheckOut() == null) {
            throw new BusinessRuleException("Actual check-out time is required.");
        }
        if (form.getActualCheckOut().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Actual check-out time cannot be in the future.");
        }
        if (!form.getActualCheckOut().isAfter(stay.getActualCheckIn())) {
            throw new BusinessRuleException("Check-out time must be after check-in time.");
        }
        Room room = roomRepository.findByIdForUpdate(stay.getRoom().getId())
                .orElseThrow(() -> new NotFoundException("Assigned room was not found."));
        BigDecimal additional = chargeRepository.totalForStay(stayId);
        stay.checkOut(form.getActualCheckOut(), additional);
        room.setStatus(RoomStatus.AVAILABLE);
        reservationService.markCheckedOut(stay.getReservation());
        audit.record(stay, "CHECK_OUT");
        notices.send(stay.getReservation().getCustomer().getUser(), "Thank you for staying. You can now leave a verified review.", "/customer/stays/" + stay.getId());
    }

    @Transactional(readOnly = true)
    public long currentCount() {
        return stayRepository.countByActualCheckOutIsNullAndVoidedFalse();
    }

    @Transactional
    public void move(Long id, Long roomId) {
        Stay stay = openStay(id);
        Long oldId = stay.getRoom().getId();
        if (oldId.equals(roomId)) throw new BusinessRuleException("Choose a different room.");
        // Stable ordering prevents two opposite room moves deadlocking each other.
        roomRepository.findByIdForUpdate(Math.min(oldId, roomId)).orElseThrow(() -> new NotFoundException("Room not found."));
        roomRepository.findByIdForUpdate(Math.max(oldId, roomId)).orElseThrow(() -> new NotFoundException("Room not found."));
        Room target = roomRepository.findByIdForUpdate(roomId).orElseThrow();
        Reservation booking = stay.getReservation();
        if (target.getStatus() != RoomStatus.AVAILABLE || !target.getRoomType().isActive()
                || target.getRoomType().getCapacity() < stay.getGuestCount())
            throw new BusinessRuleException("The destination room is unavailable or too small.");
        if (!booking.getCheckOutDate().isAfter(LocalDate.now())) throw new BusinessRuleException("Extend the booking before changing an overdue stay's room.");
        if (reservationRepository.countBlockingOverlaps(roomId, LocalDate.now(), booking.getCheckOutDate(), booking.getId()) > 0)
            throw new BusinessRuleException("The destination room has an overlapping booking.");
        if(maintenance.blocked(roomId,LocalDate.now(),booking.getCheckOutDate())) throw new BusinessRuleException("Destination has scheduled maintenance.");
        stay.getRoom().setStatus(RoomStatus.AVAILABLE);
        target.setStatus(RoomStatus.OCCUPIED);
        stay.moveTo(target);
        booking.updateDetails(target, booking.getCheckInDate(), booking.getCheckOutDate(), booking.getGuestCount(),
                booking.getNightlyPriceSnapshot(), booking.getGrossTotal(), booking.getDiscountAmount(), booking.getTotalAmount(), booking.getPromotion(), booking.getNotes());
        audit.record("Stay", id, "ROOM_CHANGE", "Room " + oldId + " -> " + roomId + "; original contracted rate retained");
    }

    @Transactional
    public void extend(Long id, LocalDate date) {
        Stay stay = openStay(id);
        Reservation booking = stay.getReservation();
        roomRepository.findByIdForUpdate(stay.getRoom().getId()).orElseThrow();
        if (date == null || !date.isAfter(booking.getCheckOutDate()) || !date.isAfter(LocalDate.now()))
            throw new BusinessRuleException("New departure must be after the existing departure and today.");
        if (reservationRepository.countBlockingOverlaps(stay.getRoom().getId(), booking.getCheckOutDate(), date, booking.getId()) > 0)
            throw new BusinessRuleException("Extension overlaps another booking.");
        if(maintenance.blocked(stay.getRoom().getId(),booking.getCheckOutDate(),date)) throw new BusinessRuleException("Extension overlaps scheduled maintenance.");
        long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckOutDate(), date);
        BigDecimal extra = booking.getNightlyPriceSnapshot().multiply(BigDecimal.valueOf(nights));
        booking.updateDetails(booking.getRoom(), booking.getCheckInDate(), date, booking.getGuestCount(),
                booking.getNightlyPriceSnapshot(), booking.getGrossTotal().add(extra), booking.getDiscountAmount(),
                booking.getTotalAmount().add(extra), booking.getPromotion(), booking.getNotes());
        stay.extendCharge(extra);
        audit.record("Stay", id, "STAY_EXTENSION", "Departure " + date + "; additional nights " + nights);
        notices.send(booking.getCustomer().getUser(), "Your stay was extended to " + date, "/customer/stays/" + id);
    }

    @Transactional
    public void voidStay(Long id) {
        Stay stay = openStay(id);
        if (!charges(id).isEmpty()) throw new BusinessRuleException("Remove incorrect charges before voiding an erroneous check-in.");
        roomRepository.findByIdForUpdate(stay.getRoom().getId()).orElseThrow();
        stay.voidRecord();
        stay.getRoom().setStatus(RoomStatus.AVAILABLE);
        stay.getReservation().transitionTo(ReservationStatus.CANCELLED, "Erroneous stay voided; rebook if needed");
        audit.record(stay, "VOID");
        audit.record(stay.getReservation(), "CANCEL");
    }

    private Stay openStay(Long stayId) {
        Stay stay = stayRepository.findDetailedByIdForUpdate(stayId)
                .orElseThrow(() -> new NotFoundException("Stay was not found."));
        if (stay.isCompleted() || stay.isVoided()) {
            throw new BusinessRuleException("Charges cannot be changed after check-out.");
        }
        return stay;
    }

    private void recalculate(Stay stay) {
        stay.updateAdditionalTotal(chargeRepository.totalForStay(stay.getId()));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
