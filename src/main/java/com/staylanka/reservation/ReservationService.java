package com.staylanka.reservation;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import com.staylanka.common.ReferenceGenerator;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.promotion.PromotionService;
import com.staylanka.promotion.PromotionUsage;
import com.staylanka.promotion.PromotionUsageRepository;
import com.staylanka.room.Room;
import com.staylanka.room.RoomRepository;
import com.staylanka.room.RoomStatus;
import com.staylanka.security.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class ReservationService {
    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = transitions();

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final CurrentUserService currentUserService;
    private final PromotionService promotionService;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                              CurrentUserService currentUserService, PromotionService promotionService,
                              PromotionUsageRepository promotionUsageRepository,
                              ReferenceGenerator referenceGenerator, ApplicationEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.currentUserService = currentUserService;
        this.promotionService = promotionService;
        this.promotionUsageRepository = promotionUsageRepository;
        this.referenceGenerator = referenceGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reservation create(Authentication authentication, ReservationForm form) {
        CustomerProfile customer = currentUserService.customer(authentication);
        Room room = lockRoom(form.getRoomId());
        Pricing pricing = validateAndPrice(room, form, null);
        Reservation reservation = new Reservation(uniqueReference(), customer, room, form.getCheckInDate(),
                form.getCheckOutDate(), form.getGuestCount(), room.getNightlyPrice(), pricing.gross(),
                pricing.promotion().discount(), pricing.total(), pricing.promotion().promotion(),
                trimToNull(form.getNotes()));
        reservationRepository.save(reservation);
        savePromotionUsage(reservation, pricing.promotion());
        return reservation;
    }

    @Transactional
    public void updateOwn(Authentication authentication, Long id, ReservationForm form) {
        Reservation reservation = own(authentication, id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessRuleException("Only pending reservations can be edited.");
        }
        Room room = lockRoom(form.getRoomId());
        Pricing pricing = validateAndPrice(room, form, id);
        reservation.updateDetails(room, form.getCheckInDate(), form.getCheckOutDate(), form.getGuestCount(),
                room.getNightlyPrice(), pricing.gross(), pricing.promotion().discount(), pricing.total(),
                pricing.promotion().promotion(), trimToNull(form.getNotes()));
        savePromotionUsage(reservation, pricing.promotion());
    }

    @Transactional(readOnly = true)
    public Page<Reservation> ownReservations(Authentication authentication, int page) {
        return reservationRepository.findByCustomerUserEmailIgnoreCase(authentication.getName(),
                PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Reservation own(Authentication authentication, Long id) {
        Reservation reservation = detailed(id);
        if (!reservation.getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new NotFoundException("Reservation was not found.");
        }
        return reservation;
    }

    @Transactional(readOnly = true)
    public Reservation detailed(Long id) {
        return reservationRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Reservation was not found."));
    }

    @Transactional(readOnly = true)
    public Page<Reservation> search(String term, ReservationStatus status, LocalDate fromDate,
                                    LocalDate toDate, int page) {
        return reservationRepository.search(term == null ? "" : term.trim(), status, fromDate, toDate,
                PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public void cancelOwn(Authentication authentication, Long id, String reason) {
        Reservation reservation = own(authentication, id);
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("A cancellation reason is required.");
        }
        transition(reservation, ReservationStatus.CANCELLED, reason.trim());
    }

    @Transactional
    public void confirm(Long id) {
        transition(detailed(id), ReservationStatus.CONFIRMED, null);
    }

    @Transactional
    public void reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("A rejection reason is required.");
        }
        transition(detailed(id), ReservationStatus.REJECTED, reason.trim());
    }

    @Transactional
    public void markNoShow(Long id, String reason) {
        transition(detailed(id), ReservationStatus.NO_SHOW,
                reason == null || reason.isBlank() ? "Guest did not arrive" : reason.trim());
    }

    @Transactional
    public void markCheckedIn(Reservation reservation) {
        transition(reservation, ReservationStatus.CHECKED_IN, null);
    }

    @Transactional
    public void markCheckedOut(Reservation reservation) {
        transition(reservation, ReservationStatus.CHECKED_OUT, null);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return reservationRepository.countByStatus(ReservationStatus.PENDING);
    }

    private Room lockRoom(Long id) {
        return roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Room was not found."));
    }

    private Pricing validateAndPrice(Room room, ReservationForm form, Long excludeReservationId) {
        if (form.getCheckInDate() == null || form.getCheckOutDate() == null) {
            throw new BusinessRuleException("Check-in and check-out dates are required.");
        }
        if (form.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("A reservation cannot start in the past.");
        }
        if (!form.getCheckOutDate().isAfter(form.getCheckInDate())) {
            throw new BusinessRuleException("Check-out must be later than check-in.");
        }
        if (form.getGuestCount() <= 0 || form.getGuestCount() > room.getRoomType().getCapacity()) {
            throw new BusinessRuleException("Guest count exceeds this room's capacity.");
        }
        if (!room.getRoomType().isActive() || room.getStatus() == RoomStatus.INACTIVE
                || room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new BusinessRuleException("This room is not available for reservation.");
        }
        if (reservationRepository.countBlockingOverlaps(room.getId(), form.getCheckInDate(),
                form.getCheckOutDate(), excludeReservationId) > 0) {
            throw new ConflictException("This room has just been reserved for overlapping dates. Please choose another room.");
        }
        long nights = ChronoUnit.DAYS.between(form.getCheckInDate(), form.getCheckOutDate());
        BigDecimal gross = room.getNightlyPrice().multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
        PromotionService.PromotionResult promotion = promotionService.apply(form.getPromotionCode(), gross, nights);
        return new Pricing(gross, gross.subtract(promotion.discount()).max(BigDecimal.ZERO), promotion);
    }

    private void transition(Reservation reservation, ReservationStatus target, String reason) {
        ReservationStatus current = reservation.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessRuleException("Reservation cannot move from " + current + " to " + target + ".");
        }
        reservation.transitionTo(target, reason);
        eventPublisher.publishEvent(new ReservationStatusChangedEvent(reservation.getId(),
                reservation.getReservationReference(), current, target));
    }

    private String uniqueReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String reference = referenceGenerator.next("SLR");
            if (!reservationRepository.existsByReservationReference(reference)) {
                return reference;
            }
        }
        throw new ConflictException("A reservation reference could not be generated. Please retry.");
    }

    private void savePromotionUsage(Reservation reservation, PromotionService.PromotionResult result) {
        var existing = promotionUsageRepository.findByReservationId(reservation.getId());
        if (result.promotion() == null) {
            existing.ifPresent(promotionUsageRepository::delete);
        } else if (existing.isPresent()) {
            existing.get().update(result.promotion(), result.discount());
        } else {
            promotionUsageRepository.save(new PromotionUsage(result.promotion(), reservation, result.discount()));
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Map<ReservationStatus, Set<ReservationStatus>> transitions() {
        Map<ReservationStatus, Set<ReservationStatus>> map = new EnumMap<>(ReservationStatus.class);
        map.put(ReservationStatus.PENDING, EnumSet.of(ReservationStatus.CONFIRMED,
                ReservationStatus.REJECTED, ReservationStatus.CANCELLED));
        map.put(ReservationStatus.CONFIRMED, EnumSet.of(ReservationStatus.CANCELLED,
                ReservationStatus.CHECKED_IN, ReservationStatus.NO_SHOW));
        map.put(ReservationStatus.CHECKED_IN, EnumSet.of(ReservationStatus.CHECKED_OUT));
        return Map.copyOf(map);
    }

    private record Pricing(BigDecimal gross, BigDecimal total,
                           PromotionService.PromotionResult promotion) {
    }
}
