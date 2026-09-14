package com.staylanka.promotion;

import com.staylanka.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "minimum_nights", nullable = false)
    private int minimumNights;

    @Column(name = "minimum_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumAmount;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Required by JPA.
     */
    protected Promotion() {
    }

    public Promotion(
            String code,
            String name,
            String description,
            PromotionType type,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            int minimumNights,
            BigDecimal minimumAmount
    ) {
        update(
                code,
                name,
                description,
                type,
                value,
                startDate,
                endDate,
                minimumNights,
                minimumAmount
        );
    }

    /**
     * Updates promotion details.
     *
     * @throws IllegalArgumentException if any promotion rule is invalid
     */
    public void update(
            String code,
            String name,
            String description,
            PromotionType type,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            int minimumNights,
            BigDecimal minimumAmount
    ) {
        validate(
                code,
                name,
                type,
                value,
                startDate,
                endDate,
                minimumNights,
                minimumAmount
        );

        this.code = normalizeCode(code);
        this.name = name.trim();
        this.description = normalizeDescription(description);
        this.type = type;
        this.value = value;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minimumNights = minimumNights;
        this.minimumAmount = minimumAmount;
    }

    private void validate(
            String code,
            String name,
            PromotionType type,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            int minimumNights,
            BigDecimal minimumAmount
    ) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Promotion code cannot be empty");
        }

        if (code.length() > 40) {
            throw new IllegalArgumentException(
                    "Promotion code cannot exceed 40 characters"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Promotion name cannot be empty");
        }

        if (name.length() > 160) {
            throw new IllegalArgumentException(
                    "Promotion name cannot exceed 160 characters"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException("Promotion type cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("Promotion value cannot be null");
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Promotion value cannot be negative"
            );
        }

        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }

        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        if (minimumNights < 0) {
            throw new IllegalArgumentException(
                    "Minimum nights cannot be negative"
            );
        }

        if (minimumAmount == null) {
            throw new IllegalArgumentException(
                    "Minimum amount cannot be null"
            );
        }

        if (minimumAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Minimum amount cannot be negative"
            );
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PromotionType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getMinimumNights() {
        return minimumNights;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Activates the promotion.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Deactivates the promotion.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Checks whether the promotion is valid for a given date.
     */
    public boolean isValidOn(LocalDate date) {
        if (!active || date == null) {
            return false;
        }

        return !date.isBefore(startDate)
                && !date.isAfter(endDate);
    }

    /**
     * Checks whether the promotion meets the minimum-night requirement.
     */
    public boolean meetsMinimumNights(int nights) {
        return nights >= minimumNights;
    }

    /**
     * Checks whether the promotion meets the minimum-amount requirement.
     */
    public boolean meetsMinimumAmount(BigDecimal amount) {
        return amount != null
                && amount.compareTo(minimumAmount) >= 0;
    }
}