package com.staylanka.room;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import com.staylanka.reservation.ReservationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.staylanka.common.AuditService audit;

    @org.springframework.beans.factory.annotation.Autowired
    private com.staylanka.common.InputRules rules;

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final ReservationRepository reservationRepository;
    private final EntityManager em;

    public RoomService(
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            RoomImageRepository imageRepository,
            FileStorageService fileStorageService,
            ReservationRepository reservationRepository,
            EntityManager em
    ) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.reservationRepository = reservationRepository;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public Page<Room> browse(
            String term,
            int page
    ) {

        return roomRepository.search(
                term == null
                        ? ""
                        : term.trim(),

                PageRequest.of(
                        Math.max(page, 0),
                        9,
                        Sort.by(
                                "roomNumber"
                        ).ascending()
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<Room> browsePublic(
            String term,
            int page
    ) {

        return roomRepository.searchPublic(
                term == null
                        ? ""
                        : term.trim(),

                PageRequest.of(
                        Math.max(page, 0),
                        9,
                        Sort.by(
                                "roomNumber"
                        ).ascending()
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<Room> available(
            RoomSearchForm form,
            int page
    ) {

        validateSearch(form);

        return roomRepository.findAvailable(
                form.getCheckIn(),
                form.getCheckOut(),
                form.getGuests(),
                form.getRoomTypeId(),
                form.getMaxPrice(),

                PageRequest.of(
                        Math.max(page, 0),
                        9,
                        Sort.by(
                                "nightlyPrice"
                        ).ascending()
                )
        );
    }

    public void validateSearch(
            RoomSearchForm form
    ) {

        if (form.getCheckIn() == null
                || form.getCheckOut() == null) {

            throw new BusinessRuleException(
                    "Both check-in and check-out dates are required."
            );
        }

        if (form.getCheckIn()
                .isBefore(LocalDate.now())) {

            throw new BusinessRuleException(
                    "Check-in cannot be in the past."
            );
        }

        if (!form.getCheckOut()
                .isAfter(form.getCheckIn())) {

            throw new BusinessRuleException(
                    "Check-out must be later than check-in."
            );
        }
    }

    @Transactional(readOnly = true)
    public Room get(
            Long id
    ) {

        return roomRepository
                .findDetailedById(id)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        "Room was not found."
                                )
                );
    }

    @Transactional(readOnly = true)
    public Room getPublic(
            Long id
    ) {

        return roomRepository
                .findPublicDetailedById(id)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        "Room was not found."
                                )
                );
    }

    @Transactional(readOnly = true)
    public List<RoomImage> images(
            Long roomId
    ) {

        return imageRepository
                .findByRoomIdOrderByPrimaryImageDescIdAsc(
                        roomId
                );
    }

    @Transactional(readOnly = true)
    public Map<Long, String> primaryImagePaths(
            List<Room> rooms
    ) {

        Map<Long, String> paths =
                new LinkedHashMap<>();

        if (rooms == null
                || rooms.isEmpty()) {

            return paths;
        }

        List<Long> roomIds =
                rooms.stream()
                        .map(Room::getId)
                        .toList();

        for (
                RoomImage image :
                imageRepository.findForRooms(
                        roomIds
                )
        ) {

            paths.putIfAbsent(
                    image.getRoom()
                            .getId(),

                    image.getStoragePath()
            );
        }

        return paths;
    }

    @Transactional(readOnly = true)
    public List<RoomType> activeTypes() {

        return roomTypeRepository
                .findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Room> selection() {

        return roomRepository.selection();
    }

    @Transactional(readOnly = true)
    public List<RoomType> allTypes() {

        return roomTypeRepository.findAll(
                Sort.by(
                        "name"
                ).ascending()
        );
    }

    @Transactional(readOnly = true)
    public RoomType getType(
            Long id
    ) {

        return roomTypeRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        "Room type was not found."
                                )
                );
    }

    @Transactional
    public RoomType createType(
            RoomTypeForm form
    ) {

        rules.validate(form);

        if (
                roomTypeRepository
                        .existsByNameIgnoreCase(
                                form.getName()
                        )
        ) {

            throw new ConflictException(
                    "A room type with this name already exists."
            );
        }

        RoomType created =
                roomTypeRepository.save(
                        new RoomType(
                                form.getName()
                                        .trim(),

                                trimToNull(
                                        form.getDescription()
                                ),

                                form.getCapacity(),

                                form.getBedInformation()
                                        .trim(),

                                form.getBasePrice(),

                                trimToNull(
                                        form.getAmenities()
                                )
                        )
                );

        audit.record(
                created,
                "CREATE"
        );

        return created;
    }

    @Transactional
    public void updateType(
            Long id,
            RoomTypeForm form
    ) {

        rules.validate(form);

        RoomType type =
                getType(id);

        if (
                roomTypeRepository
                        .existsByNameIgnoreCaseAndIdNot(
                                form.getName(),
                                id
                        )
        ) {

            throw new ConflictException(
                    "A room type with this name already exists."
            );
        }

        if (
                form.getCapacity()
                        < type.getCapacity()

                        && reservationRepository
                        .countCapacityConflictsForRoomType(
                                id,
                                form.getCapacity(),
                                LocalDate.now()
                        ) > 0
        ) {

            throw new BusinessRuleException(
                    "Room type capacity cannot be reduced below the guest " +
                            "count of an active or upcoming reservation."
            );
        }

        type.update(
                form.getName().trim(),

                trimToNull(
                        form.getDescription()
                ),

                form.getCapacity(),

                form.getBedInformation()
                        .trim(),

                form.getBasePrice(),

                trimToNull(
                        form.getAmenities()
                )
        );

        audit.record(
                type,
                "UPDATE"
        );
    }

    @Transactional
    public void toggleType(
            Long id
    ) {

        RoomType type =
                getType(id);

        if (
                type.isActive()

                        && reservationRepository
                        .countCapacityConflictsForRoomType(
                                id,
                                0,
                                LocalDate.now()
                        ) > 0
        ) {

            throw new BusinessRuleException(
                    "Resolve active or upcoming bookings before " +
                            "deactivating this room type."
            );
        }

        type.setActive(
                !type.isActive()
        );

        audit.record(
                type,
                "STATUS_CHANGE"
        );
    }

    @Transactional
    public Room create(
            RoomForm form
    ) {

        rules.validate(form);

        if (
                roomRepository
                        .existsByRoomNumberIgnoreCase(
                                form.getRoomNumber()
                        )
        ) {

            throw new ConflictException(
                    "This room number is already in use."
            );
        }

        RoomType type =
                getType(
                        form.getRoomTypeId()
                );

        if (!type.isActive()) {

            throw new BusinessRuleException(
                    "Choose an active room type for a new room."
            );
        }

        Room room =
                roomRepository.save(
                        new Room(
                                form.getRoomNumber()
                                        .trim(),

                                type,

                                trimToNull(
                                        form.getDescription()
                                ),

                                form.getNightlyPrice(),

                                RoomStatus.AVAILABLE
                        )
                );

        saveImageIfPresent(
                room,
                form
        );

        audit.record(
                room,
                "CREATE"
        );

        return room;
    }

    @Transactional
    public void update(
            Long id,
            RoomForm form
    ) {

        rules.validate(form);

        Room room =
                roomRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Room was not found."
                                        )
                        );

        if (
                roomRepository
                        .existsByRoomNumberIgnoreCaseAndIdNot(
                                form.getRoomNumber(),
                                id
                        )
        ) {

            throw new ConflictException(
                    "This room number is already in use."
            );
        }

        RoomType type =
                getType(
                        form.getRoomTypeId()
                );

        if (
                !room.getRoomType()
                        .getId()
                        .equals(type.getId())

                        && (
                        room.getStatus()
                                == RoomStatus.OCCUPIED

                                || reservationRepository
                                .countBlockingOperationalChanges(
                                        room.getId(),
                                        LocalDate.now()
                                ) > 0
                )
        ) {

            throw new BusinessRuleException(
                    "Room type cannot be changed while this room has " +
                            "an active or upcoming reservation."
            );
        }

        if (
                !type.isActive()

                        && (
                        room.getStatus()
                                == RoomStatus.AVAILABLE

                                || room.getStatus()
                                == RoomStatus.OCCUPIED
                )
        ) {

            throw new BusinessRuleException(
                    "An operational room must use an active room type."
            );
        }

        room.updateDetails(
                form.getRoomNumber()
                        .trim(),

                type,

                trimToNull(
                        form.getDescription()
                ),

                form.getNightlyPrice()
        );

        saveImageIfPresent(
                room,
                form
        );

        audit.record(
                room,
                "UPDATE"
        );
    }

    @Transactional
    public void changeOperationalStatus(
            Long id,
            RoomStatus target
    ) {

        if (target == null) {

            throw new BusinessRuleException(
                    "Choose a valid room status."
            );
        }

        Room room =
                roomRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Room was not found."
                                        )
                        );

        applyOperationalStatus(
                room,
                target
        );
    }

    /*
     * Kept only for compatibility with any existing code
     * that still calls deactivate().
     *
     * This is NOT the CRUD Delete operation.
     */
    @Transactional
    public void deactivate(
            Long id
    ) {

        Room room =
                roomRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Room was not found."
                                        )
                        );

        applyOperationalStatus(
                room,
                RoomStatus.INACTIVE
        );
    }

    /*
     * =========================================================
     * HARD DELETE ROOM
     * =========================================================
     *
     * Physically deletes the room and linked room records.
     *
     * ROOM_MANAGER and ADMIN only.
     *
     * Active operational data is protected:
     *
     * - OCCUPIED room cannot be deleted.
     * - Active/upcoming booking cannot be deleted.
     * - Active stay cannot be deleted.
     *
     * Historical records can be deleted with the room.
     */
    @Transactional
    @PreAuthorize(
            "hasAnyRole('ROOM_MANAGER','ADMIN')"
    )
    public void deletePermanently(
            Long id
    ) {

        Room room =
                roomRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Room was not found."
                                        )
                        );

        /*
         * ---------------------------------------------
         * OCCUPIED ROOM PROTECTION
         * ---------------------------------------------
         */

        if (
                room.getStatus()
                        == RoomStatus.OCCUPIED
        ) {

            throw new BusinessRuleException(
                    "An occupied room cannot be permanently deleted. " +
                            "Check out or move the guest first."
            );
        }

        /*
         * ---------------------------------------------
         * ACTIVE / UPCOMING RESERVATION PROTECTION
         * ---------------------------------------------
         */

        if (
                reservationRepository
                        .countBlockingOperationalChanges(
                                id,
                                LocalDate.now()
                        ) > 0
        ) {

            throw new BusinessRuleException(
                    "This room has an active or upcoming reservation. " +
                            "Cancel, reject, complete, or reassign it before " +
                            "permanent deletion."
            );
        }

        /*
         * ---------------------------------------------
         * ACTIVE STAY PROTECTION
         * ---------------------------------------------
         */

        Number activeStayCount =
                (Number)
                        em.createNativeQuery(
                                        """
                                        SELECT COUNT(*)
                                        FROM stays
                                        WHERE room_id = ?1
                                          AND actual_check_out IS NULL
                                          AND voided = FALSE
                                        """
                                )
                                .setParameter(
                                        1,
                                        id
                                )
                                .getSingleResult();

        if (
                activeStayCount.longValue()
                        > 0
        ) {

            throw new BusinessRuleException(
                    "This room is still linked to an active stay. " +
                            "Check out, void, or move that stay before " +
                            "permanent deletion."
            );
        }

        /*
         * Keep physical image paths so we can remove
         * uploaded files after the DB transaction commits.
         */
        List<RoomImage> images =
                imageRepository
                        .findByRoomIdOrderByPrimaryImageDescIdAsc(
                                id
                        );

        List<String> imagePaths =
                images.stream()
                        .map(
                                RoomImage::getStoragePath
                        )
                        .toList();

        /*
         * =====================================================
         * REQUEST HISTORY
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM request_history
                        WHERE request_id IN (
                            SELECT gr.id
                            FROM guest_requests gr
                            JOIN reservations r
                              ON r.id = gr.reservation_id
                            WHERE r.room_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * REQUEST RESPONSES
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM request_responses
                        WHERE request_id IN (
                            SELECT gr.id
                            FROM guest_requests gr
                            JOIN reservations r
                              ON r.id = gr.reservation_id
                            WHERE r.room_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * GUEST REQUESTS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM guest_requests
                        WHERE reservation_id IN (
                            SELECT r.id
                            FROM reservations r
                            WHERE r.room_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * REVIEWS
         * =====================================================
         *
         * Review references a Stay.
         */

        em.createNativeQuery(
                        """
                        DELETE FROM reviews
                        WHERE stay_id IN (
                            SELECT s.id
                            FROM stays s
                            JOIN reservations r
                              ON r.id = s.reservation_id
                            WHERE s.room_id = ?1
                               OR r.room_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * ADDITIONAL CHARGES
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM additional_charges
                        WHERE stay_id IN (
                            SELECT s.id
                            FROM stays s
                            JOIN reservations r
                              ON r.id = s.reservation_id
                            WHERE s.room_id = ?1
                               OR r.room_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * STAYS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM stays
                        WHERE room_id = ?1
                           OR reservation_id IN (
                               SELECT r.id
                               FROM reservations r
                               WHERE r.room_id = ?1
                           )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * PROMOTION USAGE
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM promotion_usages
                        WHERE reservation_id IN (
                            SELECT r.id
                            FROM reservations r
                            WHERE r.room_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * RESERVATIONS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM reservations
                        WHERE room_id = ?1
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * MAINTENANCE BLOCKS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM maintenance_blocks
                        WHERE room_id = ?1
                        """
                )
                .setParameter(
                        1,
                        id
                )
                .executeUpdate();

        /*
         * =====================================================
         * ROOM IMAGE DATABASE ROWS
         * =====================================================
         */

        if (!images.isEmpty()) {

            imageRepository.deleteAll(
                    images
            );

            imageRepository.flush();
        }

        /*
         * Audit BEFORE room deletion because the audit table
         * stores entity type/id as historical information.
         */
        audit.record(
                "Room",
                id,
                "HARD_DELETE",
                "Room and linked room history permanently removed"
        );

        /*
         * =====================================================
         * DELETE ROOM
         * =====================================================
         */

        roomRepository.delete(
                room
        );

        roomRepository.flush();

        /*
         * Remove physical image files after the DB transaction
         * successfully commits.
         */
        registerCommitCleanup(
                imagePaths
        );
    }

    @Transactional(readOnly = true)
    public long countAvailable() {

        return roomRepository
                .countByStatus(
                        RoomStatus.AVAILABLE
                );
    }

    private void applyOperationalStatus(
            Room room,
            RoomStatus target
    ) {

        if (
                target
                        == RoomStatus.OCCUPIED
        ) {

            throw new BusinessRuleException(
                    "Occupied status is controlled by the check-in workflow."
            );
        }

        if (
                room.getStatus()
                        == RoomStatus.OCCUPIED
        ) {

            throw new BusinessRuleException(
                    "An occupied room's status is controlled by check-in and check-out."
            );
        }

        if (
                target
                        == RoomStatus.AVAILABLE

                        && !room.getRoomType()
                        .isActive()
        ) {

            throw new BusinessRuleException(
                    "Activate the room type before making this room available."
            );
        }

        if (
                (
                        target == RoomStatus.INACTIVE
                                || target == RoomStatus.MAINTENANCE
                )

                        && reservationRepository
                        .countBlockingOperationalChanges(
                                room.getId(),
                                LocalDate.now()
                        ) > 0
        ) {

            throw new BusinessRuleException(
                    "This room has an active or upcoming reservation. " +
                            "Cancel, reject, or reassign it before changing " +
                            "the room status."
            );
        }

        room.setStatus(
                target
        );

        audit.record(
                room,
                "STATUS_CHANGE"
        );
    }

    private void saveImageIfPresent(
            Room room,
            RoomForm form
    ) {

        if (
                form.getImage() == null
                        || form.getImage().isEmpty()
        ) {

            return;
        }

        FileStorageService.StoredFile stored =
                fileStorageService
                        .storeRoomImage(
                                form.getImage()
                        );

        registerRollbackCleanup(
                stored.relativePath()
        );

        boolean primary =
                !imageRepository
                        .existsByRoomId(
                                room.getId()
                        );

        imageRepository.save(
                new RoomImage(
                        room,
                        stored.originalName(),
                        stored.contentType(),
                        stored.relativePath(),
                        primary
                )
        );
    }

    private void registerRollbackCleanup(
            String relativePath
    ) {

        if (
                !TransactionSynchronizationManager
                        .isActualTransactionActive()
        ) {

            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCompletion(
                                    int status
                            ) {

                                if (
                                        status
                                                != TransactionSynchronization
                                                .STATUS_COMMITTED
                                ) {

                                    fileStorageService.delete(
                                            relativePath
                                    );
                                }
                            }
                        }
                );
    }

    private void registerCommitCleanup(
            List<String> relativePaths
    ) {

        if (
                relativePaths == null
                        || relativePaths.isEmpty()
        ) {

            return;
        }

        if (
                !TransactionSynchronizationManager
                        .isActualTransactionActive()
        ) {

            relativePaths.forEach(
                    fileStorageService::delete
            );

            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                relativePaths.forEach(
                                        fileStorageService::delete
                                );
                            }
                        }
                );
    }

    private String trimToNull(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? null
                : value.trim();
    }
}