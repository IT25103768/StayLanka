package com.staylanka.common;

import com.staylanka.customer.CustomerProfile;
import com.staylanka.promotion.Promotion;
import com.staylanka.request.GuestRequest;
import com.staylanka.request.RequestStatus;
import com.staylanka.reservation.Reservation;
import com.staylanka.reservation.ReservationStatus;
import com.staylanka.review.Review;
import com.staylanka.review.ReviewStatus;
import com.staylanka.room.Room;
import com.staylanka.room.RoomStatus;
import com.staylanka.stay.Stay;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PurgeService {

    public static final Map<String, Class<? extends BaseEntity>> TYPES =
            Map.of(
                    "Room", Room.class,
                    "Reservation", Reservation.class,
                    "CustomerProfile", CustomerProfile.class,
                    "Stay", Stay.class,
                    "Promotion", Promotion.class,
                    "Review", Review.class,
                    "GuestRequest", GuestRequest.class
            );

    private final EntityManager em;
    private final AuditService audit;

    public PurgeService(
            EntityManager em,
            AuditService audit
    ) {
        this.em = em;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<?> rows(String type) {

        requireType(type);

        String label = switch (type) {
            case "Room" ->
                    "e.roomNumber";

            case "Reservation" ->
                    "e.reservationReference";

            case "CustomerProfile" ->
                    "concat(e.firstName, ' ', e.lastName)";

            case "Stay" ->
                    "e.reservation.reservationReference";

            case "Promotion" ->
                    "e.code";

            case "Review" ->
                    "e.status";

            default ->
                    "e.requestReference";
        };

        return em.createQuery(
                        "select e.id, " +
                                label +
                                " from " +
                                type +
                                " e order by e.id desc"
                )
                .setMaxResults(100)
                .getResultList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            String type,
            Long id,
            String confirmation
    ) {

        requireType(type);

        String expectedConfirmation =
                "DELETE " + type + " " + id;

        if (!expectedConfirmation.equals(confirmation)) {
            throw new BusinessRuleException(
                    "Type the exact confirmation phrase."
            );
        }

        BaseEntity entity =
                em.find(
                        TYPES.get(type),
                        id,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (entity == null) {
            throw new NotFoundException(
                    "Record not found."
            );
        }


        /*
         * =====================================================
         * ROOM
         * =====================================================
         */

        if (entity instanceof Room room) {

            require(
                    room.getStatus() == RoomStatus.INACTIVE,
                    "Deactivate the room before permanent deletion."
            );

            absent(
                    "Reservation",
                    "room.id",
                    id
            );

            absent(
                    "Stay",
                    "room.id",
                    id
            );

            removeChildren(
                    "MaintenanceBlock",
                    "room.id",
                    id
            );

            removeChildren(
                    "RoomImage",
                    "room.id",
                    id
            );
        }


        /*
         * =====================================================
         * RESERVATION
         * =====================================================
         */

        else if (entity instanceof Reservation booking) {

            require(
                    Set.of(
                            ReservationStatus.CANCELLED,
                            ReservationStatus.REJECTED,
                            ReservationStatus.NO_SHOW,
                            ReservationStatus.CHECKED_OUT
                    ).contains(booking.getStatus()),

                    "Cancel or complete the reservation first."
            );

            absent(
                    "Stay",
                    "reservation.id",
                    id
            );

            absent(
                    "GuestRequest",
                    "reservation.id",
                    id
            );

            removeChildren(
                    "PromotionUsage",
                    "reservation.id",
                    id
            );
        }


        /*
         * =====================================================
         * CUSTOMER
         * =====================================================
         *
         * Permanent customer deletion now removes all eligible
         * historical records belonging to that customer.
         *
         * Active operational records still block deletion.
         */

        else if (entity instanceof CustomerProfile profile) {

            deleteCustomerAndHistory(profile);

            /*
             * Customer deletion is performed completely inside
             * deleteCustomerAndHistory(), including AppUser.
             *
             * Skip the generic em.remove(entity) below.
             */
        }


        /*
         * =====================================================
         * STAY
         * =====================================================
         */

        else if (entity instanceof Stay stay) {

            require(
                    stay.isVoided() || stay.isCompleted(),
                    "Void or complete the stay first."
            );

            absent(
                    "Review",
                    "stay.id",
                    id
            );

            removeChildren(
                    "AdditionalCharge",
                    "stay.id",
                    id
            );
        }


        /*
         * =====================================================
         * PROMOTION
         * =====================================================
         */

        else if (entity instanceof Promotion promotion) {

            require(
                    !promotion.isActive(),
                    "Deactivate the promotion first."
            );

            absent(
                    "Reservation",
                    "promotion.id",
                    id
            );

            absent(
                    "PromotionUsage",
                    "promotion.id",
                    id
            );
        }


        /*
         * =====================================================
         * REVIEW
         * =====================================================
         */

        else if (entity instanceof Review review) {

            require(
                    review.getStatus() != ReviewStatus.APPROVED,
                    "Hide the review before permanent deletion."
            );
        }


        /*
         * =====================================================
         * GUEST REQUEST
         * =====================================================
         */

        else if (entity instanceof GuestRequest request) {

            require(
                    request.isArchived()
                            || Set.of(
                            RequestStatus.CLOSED,
                            RequestStatus.CANCELLED
                    ).contains(request.getStatus()),

                    "Close, cancel or archive the request first."
            );

            removeChildren(
                    "RequestResponse",
                    "request.id",
                    id
            );

            removeChildren(
                    "RequestHistory",
                    "request.id",
                    id
            );
        }


        /*
         * CustomerProfile is already removed by
         * deleteCustomerAndHistory().
         */
        if (!(entity instanceof CustomerProfile)) {

            em.remove(entity);
        }

        em.flush();

        audit.record(
                type,
                id,
                "HARD_DELETE",
                "Administrative purge; row permanently removed"
        );
    }


    /*
     * =========================================================
     * PERMANENT CUSTOMER DELETE
     * =========================================================
     */

    private void deleteCustomerAndHistory(
            CustomerProfile profile
    ) {

        Long customerId = profile.getId();
        Long userId = profile.getUser().getId();


        /*
         * Account must first be deactivated.
         */
        require(
                !profile.getUser().isActive(),
                "Deactivate the customer first."
        );


        /*
         * -----------------------------------------------------
         * BLOCK ACTIVE RESERVATIONS
         * -----------------------------------------------------
         *
         * Historical/finished reservations can be purged.
         *
         * We do NOT automatically destroy reservations that are
         * currently pending, confirmed or checked in.
         */

        Long activeReservations =
                em.createQuery(
                                """
                                select count(r)
                                from Reservation r
                                where r.customer.id = :customerId
                                  and r.status in :statuses
                                """,
                                Long.class
                        )
                        .setParameter(
                                "customerId",
                                customerId
                        )
                        .setParameter(
                                "statuses",
                                Set.of(
                                        ReservationStatus.PENDING,
                                        ReservationStatus.CONFIRMED,
                                        ReservationStatus.CHECKED_IN
                                )
                        )
                        .getSingleResult();

        require(
                activeReservations == 0,
                "Cannot permanently delete this customer while an active reservation exists. " +
                        "Cancel, reject, complete, or mark the reservation as no-show first."
        );


        /*
         * -----------------------------------------------------
         * BLOCK ACTIVE STAYS
         * -----------------------------------------------------
         */

        Long activeStays =
                em.createQuery(
                                """
                                select count(s)
                                from Stay s
                                where s.reservation.customer.id = :customerId
                                  and s.actualCheckOut is null
                                  and s.voided = false
                                """,
                                Long.class
                        )
                        .setParameter(
                                "customerId",
                                customerId
                        )
                        .getSingleResult();

        require(
                activeStays == 0,
                "Cannot permanently delete this customer while an active stay exists. " +
                        "Check out or void the stay first."
        );


        /*
         * -----------------------------------------------------
         * BLOCK OPEN GUEST REQUESTS
         * -----------------------------------------------------
         */

        Long openRequests =
                em.createQuery(
                                """
                                select count(r)
                                from GuestRequest r
                                where r.customer.id = :customerId
                                  and r.status not in :closedStatuses
                                """,
                                Long.class
                        )
                        .setParameter(
                                "customerId",
                                customerId
                        )
                        .setParameter(
                                "closedStatuses",
                                Set.of(
                                        RequestStatus.CLOSED,
                                        RequestStatus.CANCELLED
                                )
                        )
                        .getSingleResult();

        require(
                openRequests == 0,
                "Cannot permanently delete this customer while an open guest request exists. " +
                        "Close or cancel the request first."
        );


        /*
         * =====================================================
         * DELETE REQUEST HISTORY / RESPONSES
         * =====================================================
         *
         * These must go before guest_requests because their
         * foreign keys point to guest_requests.
         */

        em.createNativeQuery(
                        """
                        DELETE FROM request_history
                        WHERE request_id IN (
                            SELECT id
                            FROM guest_requests
                            WHERE customer_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        em.createNativeQuery(
                        """
                        DELETE FROM request_responses
                        WHERE request_id IN (
                            SELECT id
                            FROM guest_requests
                            WHERE customer_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * If this customer account ever appeared as an actor/
         * author, remove those references before AppUser delete.
         */

        em.createNativeQuery(
                        """
                        DELETE FROM request_history
                        WHERE changed_by_id = ?1
                        """
                )
                .setParameter(
                        1,
                        userId
                )
                .executeUpdate();


        em.createNativeQuery(
                        """
                        DELETE FROM request_responses
                        WHERE author_id = ?1
                        """
                )
                .setParameter(
                        1,
                        userId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE GUEST REQUESTS
         * =====================================================
         *
         * Do this before reservations because a guest request
         * may reference a reservation.
         */

        em.createNativeQuery(
                        """
                        DELETE FROM guest_requests
                        WHERE customer_id = ?1
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE REVIEWS
         * =====================================================
         *
         * Reviews reference both customer_profiles and stays.
         */

        em.createNativeQuery(
                        """
                        DELETE FROM reviews
                        WHERE customer_id = ?1
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE ADDITIONAL CHARGES
         * =====================================================
         *
         * additional_charges -> stays -> reservations -> customer
         */

        em.createNativeQuery(
                        """
                        DELETE FROM additional_charges
                        WHERE stay_id IN (
                            SELECT s.id
                            FROM stays s
                            JOIN reservations r
                              ON r.id = s.reservation_id
                            WHERE r.customer_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE STAYS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM stays
                        WHERE reservation_id IN (
                            SELECT id
                            FROM reservations
                            WHERE customer_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE PROMOTION USAGE HISTORY
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM promotion_usages
                        WHERE reservation_id IN (
                            SELECT id
                            FROM reservations
                            WHERE customer_id = ?1
                        )
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE RESERVATIONS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM reservations
                        WHERE customer_id = ?1
                        """
                )
                .setParameter(
                        1,
                        customerId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE ACCOUNT-RELATED RECORDS
         * =====================================================
         */

        em.createNativeQuery(
                        """
                        DELETE FROM notifications
                        WHERE recipient_id = ?1
                        """
                )
                .setParameter(
                        1,
                        userId
                )
                .executeUpdate();


        em.createNativeQuery(
                        """
                        DELETE FROM password_recovery
                        WHERE user_id = ?1
                        """
                )
                .setParameter(
                        1,
                        userId
                )
                .executeUpdate();


        /*
         * =====================================================
         * DELETE CUSTOMER PROFILE
         * =====================================================
         */

        var user = profile.getUser();

        em.remove(profile);

        /*
         * Flush first because customer_profiles.user_id points
         * to app_users with ON DELETE RESTRICT.
         */
        em.flush();


        /*
         * =====================================================
         * DELETE LOGIN ACCOUNT
         * =====================================================
         */

        em.remove(user);

        em.flush();
    }


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private void requireType(String type) {

        if (!TYPES.containsKey(type)) {

            throw new NotFoundException(
                    "Unknown module."
            );
        }
    }

    private void require(
            boolean condition,
            String message
    ) {

        if (!condition) {

            throw new BusinessRuleException(
                    message
            );
        }
    }

    private void absent(
            String entity,
            String field,
            Long id
    ) {

        Long count =
                em.createQuery(
                                "select count(e) from "
                                        + entity
                                        + " e where e."
                                        + field
                                        + " = :id",
                                Long.class
                        )
                        .setParameter(
                                "id",
                                id
                        )
                        .getSingleResult();

        require(
                count == 0,
                "Cannot permanently delete: linked "
                        + entity
                        + " history exists. "
                        + "Retain this record or purge eligible dependent records first."
        );
    }

    private void removeChildren(
            String entity,
            String field,
            Long id
    ) {

        em.createQuery(
                        "delete from "
                                + entity
                                + " e where e."
                                + field
                                + " = :id"
                )
                .setParameter(
                        "id",
                        id
                )
                .executeUpdate();
    }
}