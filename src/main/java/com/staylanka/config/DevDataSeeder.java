package com.staylanka.config;

import com.staylanka.promotion.Promotion;
import com.staylanka.promotion.PromotionRepository;
import com.staylanka.promotion.PromotionType;
import com.staylanka.room.Room;
import com.staylanka.room.RoomRepository;
import com.staylanka.room.RoomStatus;
import com.staylanka.room.RoomType;
import com.staylanka.room.RoomTypeRepository;
import com.staylanka.user.AppUser;
import com.staylanka.user.AppUserRepository;
import com.staylanka.user.Role;
import com.staylanka.user.StaffProfile;
import com.staylanka.user.StaffProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DevDataSeeder.class);

    private final StayLankaProperties properties;
    private final AppUserRepository userRepository;
    private final StaffProfileRepository staffRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PromotionRepository promotionRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            StayLankaProperties properties,
            AppUserRepository userRepository,
            StaffProfileRepository staffRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            PromotionRepository promotionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.promotionRepository = promotionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!properties.seed().enabled()) {
            return;
        }

        seedAccounts();
        seedRooms();
        seedPromotion();
    }

    /*
     * =========================================================
     * ACCOUNTS
     * =========================================================
     */

    private void seedAccounts() {

        seedAdmin();

        seedModuleOwner(
                "IT25103762",
                properties.seed().staffPassword(),
                Role.ROOM_MANAGER,
                "Abeyrathna",
                "A.H.M.P.M.",
                "Room & Availability Manager"
        );

        seedModuleOwner(
                "IT25103763",
                properties.seed().staffPassword(),
                Role.RESERVATION_MANAGER,
                "Wimukthi",
                "A.K.A.",
                "Reservation Manager"
        );

        seedModuleOwner(
                "IT25103764",
                properties.seed().staffPassword(),
                Role.PROFILE_MANAGER,
                "Silva",
                "Y.H.S.D.",
                "Customer Profile Manager"
        );

        seedModuleOwner(
                "IT25103765",
                properties.seed().staffPassword(),
                Role.STAY_MANAGER,
                "Parindya",
                "R.K.M.",
                "Check-In/Out & Stay Manager"
        );

        seedModuleOwner(
                "IT25103767",
                properties.seed().staffPassword(),
                Role.PROMOTION_REVIEW_MANAGER,
                "Gunaseela",
                "Y.P.S.",
                "Promotion & Review Manager"
        );

        seedModuleOwner(
                "IT25103768",
                properties.seed().staffPassword(),
                Role.INQUIRY_REQUEST_MANAGER,
                "Kasthuriarachchi",
                "K.A.M.H.N.",
                "Inquiry & Special Request Manager"
        );
    }

    /*
     * =========================================================
     * ADMIN
     * =========================================================
     */

    private void seedAdmin() {

        String email =
                properties.seed().adminEmail();

        String rawPassword =
                properties.seed().adminPassword();

        if (!hasText(email) || !hasText(rawPassword)) {
            return;
        }

        AppUser admin =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElse(null);

        if (admin == null) {

            admin =
                    new AppUser(
                            email,
                            passwordEncoder.encode(rawPassword),
                            Role.ADMIN
                    );

            admin.setActive(true);

            userRepository.save(admin);

            log.info(
                    "Development administrator account created: {}",
                    email
            );

            return;
        }

        /*
         * For DEV/demo only:
         * reset the configured admin password each startup.
         */
        admin.setPasswordHash(
                passwordEncoder.encode(rawPassword)
        );

        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);

        log.info(
                "Development administrator account refreshed: {}",
                email
        );
    }

    /*
     * =========================================================
     * MODULE OWNER ACCOUNT
     * =========================================================
     *
     * IMPORTANT:
     *
     * In the old version, if the account already existed,
     * changing DEV_STAFF_PASSWORD did nothing.
     *
     * This DEV-only version refreshes the password, role and
     * active state every time the application starts.
     *
     * Therefore all six module owners can always use the
     * configured DEV_STAFF_PASSWORD during the demonstration.
     */

    private void seedModuleOwner(
            String username,
            String rawPassword,
            Role role,
            String firstName,
            String lastName,
            String jobTitle
    ) {

        if (!hasText(rawPassword)) {

            log.warn(
                    "Module owner {} was not seeded because " +
                            "DEV_STAFF_PASSWORD is empty.",
                    username
            );

            return;
        }

        AppUser staff =
                userRepository
                        .findByEmailIgnoreCase(username)
                        .orElse(null);

        /*
         * Existing account:
         * reset password and ensure correct role.
         */
        if (staff != null) {

            staff.setPasswordHash(
                    passwordEncoder.encode(rawPassword)
            );

            staff.setRole(role);
            staff.setActive(true);

            userRepository.save(staff);

            StaffProfile profile =
                    staffRepository
                            .findByUserId(staff.getId())
                            .orElse(null);

            if (profile == null) {

                staffRepository.save(
                        new StaffProfile(
                                staff,
                                firstName,
                                lastName,
                                jobTitle
                        )
                );

            } else {

                profile.update(
                        firstName,
                        lastName,
                        jobTitle
                );

                staffRepository.save(profile);
            }

            log.info(
                    "Development module-owner account refreshed: {} ({})",
                    username,
                    role
            );

            return;
        }

        /*
         * New account.
         */
        staff =
                userRepository.save(
                        new AppUser(
                                username,
                                passwordEncoder.encode(rawPassword),
                                role
                        )
                );

        staff.setActive(true);

        userRepository.save(staff);

        staffRepository.save(
                new StaffProfile(
                        staff,
                        firstName,
                        lastName,
                        jobTitle
                )
        );

        log.info(
                "Development module-owner account created: {} ({})",
                username,
                role
        );
    }

    /*
     * =========================================================
     * ROOMS
     * =========================================================
     */

    private void seedRooms() {

        if (roomTypeRepository.count() > 0) {
            return;
        }

        RoomType deluxe =
                roomTypeRepository.save(
                        new RoomType(
                                "Deluxe Garden Room",
                                "A quiet room overlooking a tropical garden.",
                                2,
                                "One king bed",
                                new BigDecimal("18500.00"),
                                "Air conditioning, Wi-Fi, breakfast, garden balcony"
                        )
                );

        RoomType family =
                roomTypeRepository.save(
                        new RoomType(
                                "Family Ocean Suite",
                                "A spacious suite for families with an Indian Ocean view.",
                                5,
                                "One king bed and three single beds",
                                new BigDecimal("32500.00"),
                                "Ocean view, Wi-Fi, breakfast, minibar, balcony"
                        )
                );

        RoomType heritage =
                roomTypeRepository.save(
                        new RoomType(
                                "Heritage Twin Room",
                                "Sri Lankan-inspired interiors with modern comfort.",
                                2,
                                "Two single beds",
                                new BigDecimal("14500.00"),
                                "Air conditioning, Wi-Fi, tea station"
                        )
                );

        roomRepository.save(
                new Room(
                        "G101",
                        deluxe,
                        "Ground-floor room near the courtyard.",
                        new BigDecimal("18500.00"),
                        RoomStatus.AVAILABLE
                )
        );

        roomRepository.save(
                new Room(
                        "G102",
                        deluxe,
                        "Upper-floor garden-facing room.",
                        new BigDecimal("19500.00"),
                        RoomStatus.AVAILABLE
                )
        );

        roomRepository.save(
                new Room(
                        "O201",
                        family,
                        "Panoramic ocean-facing family suite.",
                        new BigDecimal("32500.00"),
                        RoomStatus.AVAILABLE
                )
        );

        roomRepository.save(
                new Room(
                        "H105",
                        heritage,
                        "Heritage-style room near the lobby.",
                        new BigDecimal("14500.00"),
                        RoomStatus.AVAILABLE
                )
        );
    }

    /*
     * =========================================================
     * PROMOTION
     * =========================================================
     */

    private void seedPromotion() {

        if (promotionRepository.count() > 0) {
            return;
        }

        LocalDate today =
                LocalDate.now();

        promotionRepository.save(
                new Promotion(
                        "AYUBOWAN10",
                        "Ayubowan Welcome Offer",
                        "Save 10% on stays of two nights or more.",
                        PromotionType.PERCENTAGE,
                        new BigDecimal("10.00"),
                        today.minusDays(1),
                        today.plusMonths(6),
                        2,
                        new BigDecimal("25000.00")
                )
        );
    }

    /*
     * =========================================================
     * HELPER
     * =========================================================
     */

    private boolean hasText(String value) {

        return value != null
                && !value.isBlank();
    }
}