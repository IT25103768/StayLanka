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
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final StayLankaProperties properties;
    private final AppUserRepository userRepository;
    private final StaffProfileRepository staffRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PromotionRepository promotionRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(StayLankaProperties properties, AppUserRepository userRepository,
                         StaffProfileRepository staffRepository, RoomTypeRepository roomTypeRepository,
                         RoomRepository roomRepository, PromotionRepository promotionRepository,
                         PasswordEncoder passwordEncoder) {
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

    private void seedAccounts() {
        if (hasText(properties.seed().adminPassword())
                && !userRepository.existsByEmailIgnoreCase(properties.seed().adminEmail())) {
            userRepository.save(new AppUser(properties.seed().adminEmail(),
                    passwordEncoder.encode(properties.seed().adminPassword()), Role.ADMIN));
            log.info("Development administrator account created for configured email.");
        }
        if (hasText(properties.seed().staffPassword())
                && !userRepository.existsByEmailIgnoreCase(properties.seed().staffEmail())) {
            AppUser staff = userRepository.save(new AppUser(properties.seed().staffEmail(),
                    passwordEncoder.encode(properties.seed().staffPassword()), Role.STAFF));
            staffRepository.save(new StaffProfile(staff, "Nimali", "Perera", "Front Office Executive"));
            log.info("Development staff account created for configured email.");
        }
        if (!hasText(properties.seed().adminPassword()) || !hasText(properties.seed().staffPassword())) {
            log.warn("Development seed passwords are not configured; missing seed accounts were skipped.");
        }
    }

    private void seedRooms() {
        if (roomTypeRepository.count() > 0) {
            return;
        }
        RoomType deluxe = roomTypeRepository.save(new RoomType("Deluxe Garden Room",
                "A quiet room overlooking a tropical garden.", 2, "One king bed",
                new BigDecimal("18500.00"), "Air conditioning, Wi-Fi, breakfast, garden balcony"));
        RoomType family = roomTypeRepository.save(new RoomType("Family Ocean Suite",
                "A spacious suite for families with an Indian Ocean view.", 5, "One king bed and three single beds",
                new BigDecimal("32500.00"), "Ocean view, Wi-Fi, breakfast, minibar, balcony"));
        RoomType heritage = roomTypeRepository.save(new RoomType("Heritage Twin Room",
                "Sri Lankan-inspired interiors with modern comfort.", 2, "Two single beds",
                new BigDecimal("14500.00"), "Air conditioning, Wi-Fi, tea station"));

        roomRepository.save(new Room("G101", deluxe, "Ground-floor room near the courtyard.",
                new BigDecimal("18500.00"), RoomStatus.AVAILABLE));
        roomRepository.save(new Room("G102", deluxe, "Upper-floor garden-facing room.",
                new BigDecimal("19500.00"), RoomStatus.AVAILABLE));
        roomRepository.save(new Room("O201", family, "Panoramic ocean-facing family suite.",
                new BigDecimal("32500.00"), RoomStatus.AVAILABLE));
        roomRepository.save(new Room("H105", heritage, "Heritage-style room near the lobby.",
                new BigDecimal("14500.00"), RoomStatus.AVAILABLE));
    }

    private void seedPromotion() {
        if (promotionRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        promotionRepository.save(new Promotion("AYUBOWAN10", "Ayubowan Welcome Offer",
                "Save 10% on stays of two nights or more.", PromotionType.PERCENTAGE,
                new BigDecimal("10.00"), today.minusDays(1), today.plusMonths(6),
                2, new BigDecimal("25000.00")));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
