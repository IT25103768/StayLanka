```java
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

    private static final Map<ReservationStatus, Set<ReservationStatus>>
            ALLOWED_TRANSITIONS = transitions();

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final CurrentUserService currentUserService;
    private final PromotionService promotionService;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            CurrentUserService currentUserService,
            PromotionService promotionService,
            PromotionUsageRepository promotionUsageRepository,
            ReferenceGenerator referenceGenerator,
            ApplicationEventPublisher eventPublisher) {

        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.currentUserService = currentUserService;
        this.promotionService = promotionService;
        this.promotionUsageRepository = promotionUsageRepository;
        this.referenceGenerator = referenceGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reservation create(
            Authentication authentication,
            ReservationForm form) {

        CustomerProfile customer = currentUserService.customer(authentication);
        Room room = lockRoom(form.getRoomId());

        LocalDate today = LocalDate.now();

        Pricing pricing = validateAndPrice(
                room,
                form,
                null,
                today
        );

        Reservation reservation = new Reservation(
                uniqueReference(),
                customer,
                room,
                form.getCheckInDate(),
                form.getCheckOutDate(),
                form.getGuestCount(),
                room.getNightlyPrice(),
                pricing.gross(),
                pricing.promotion().discount(),
                pricing.total(),
                pricing.promotion().promotion(),
                trimToNull(form.getNotes())
        );

        reservationRepository.save(reservation);
        savePromotionUsage(reservation, pricing.promotion());

        return reservation;
    }

    @Transactional
    public void updateOwn(
            Authentication authentication,
            Long id,
            ReservationForm form) {

        Reservation reservation = own(authentication, id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only pending reservations can be edited."
            );
        }

        Room room = lockRoom(form.getRoomId());

        LocalDate today = LocalDate.now();

        Pricing pricing = validateAndPrice(
                room,
                form,
                id,
                today
        );

        reservation.updateDetails(
                room,
                form.getCheckInDate(),
                form.getCheckOutDate(),
                form.getGuestCount(),
                room.getNightlyPrice(),
                pricing.gross(),
                pricing.promotion().discount(),
                pricing.total(),
                pricing.promotion().promotion(),
                trimToNull(form.getNotes())
        );

        savePromotionUsage(reservation, pricing.promotion());
    }

    @Transactional(readOnly = true)
    public Page<Reservation> ownReservations(
            Authentication authentication,
            int page) {

        return reservationRepository.findByCustomerUserEmailIgnoreCase(
                authentication.getName(),
                PageRequest.of(
                        Math.max(page, 0),
                        12,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }

    @Transactional(readOnly = true
```
