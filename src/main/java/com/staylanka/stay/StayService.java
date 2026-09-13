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
import java.util.List;

@Service
public class StayService {
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
                : stayRepository.findByActualCheckOutIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public List<AdditionalCharge> charges(Long stayId) {
        return chargeRepository.findByStayIdOrderByCreatedAtAsc(stayId);
    }

    @Transactional
    public void addCharge(Long stayId, AdditionalChargeForm form) {
        Stay stay = openStay(stayId);
        chargeRepository.save(new AdditionalCharge(stay, form.getDescription().trim(),
                form.getQuantity(), form.getUnitPrice()));
        chargeRepository.flush();
        recalculate(stay);
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
        Stay stay = openStay(stayId);
        AdditionalCharge charge = getCharge(stayId, chargeId);
        charge.update(form.getDescription().trim(), form.getQuantity(), form.getUnitPrice());
        chargeRepository.flush();
        recalculate(stay);
    }

    @Transactional
    public void deleteCharge(Long stayId, Long chargeId) {
        Stay stay = openStay(stayId);
        AdditionalCharge charge = getCharge(stayId, chargeId);
        chargeRepository.delete(charge);
        chargeRepository.flush();
        recalculate(stay);
    }

    @Transactional
    public void checkOut(Long stayId, CheckOutForm form) {
        Stay stay = stayRepository.findDetailedByIdForUpdate(stayId)
                .orElseThrow(() -> new NotFoundException("Stay was not found."));
        if (stay.isCompleted()) {
            throw new BusinessRuleException("This stay has already been checked out.");
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
    }

    @Transactional(readOnly = true)
    public long currentCount() {
        return stayRepository.countByActualCheckOutIsNull();
    }

    private Stay openStay(Long stayId) {
        Stay stay = stayRepository.findDetailedByIdForUpdate(stayId)
                .orElseThrow(() -> new NotFoundException("Stay was not found."));
        if (stay.isCompleted()) {
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
