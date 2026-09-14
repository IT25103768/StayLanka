package com.staylanka.promotion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PromotionForm {

    @NotBlank(message = "Promotion code is required")
    @Pattern(
            regexp = "^[A-Za-z0-9-]{3,40}$",
            message = "Code must contain 3-40 letters, numbers, or hyphens"
    )
    private String code;

    @NotBlank(message = "Promotion name is required")
    @Size(
            max = 160,
            message = "Promotion name cannot exceed 160 characters"
    )
    private String name;

    @Size(
            max = 2000,
            message = "Description cannot exceed 2000 characters"
    )
    private String description;

    @NotNull(message = "Promotion type is required")
    private PromotionType type;

    @NotNull(message = "Promotion value is required")
    @DecimalMin(
            value = "0.01",
            message = "Promotion value must be greater than 0"
    )
    private BigDecimal value;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Min(
            value = 1,
            message = "Minimum nights must be at least 1"
    )
    private int minimumNights = 1;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(
            value = "0.00",
            message = "Minimum amount cannot be negative"
    )
    private BigDecimal minimumAmount = BigDecimal.ZERO;

    /**
     * Converts a Promotion entity into a PromotionForm.
     */
    public static PromotionForm from(Promotion promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException(
                    "Promotion cannot be null"
            );
        }

        PromotionForm form = new PromotionForm();

        form.code = promotion.getCode();
        form.name = promotion.getName();
        form.description = promotion.getDescription();
        form.type = promotion.getType();
        form.value = promotion.getValue();
        form.startDate = promotion.getStartDate();
        form.endDate = promotion.getEndDate();
        form.minimumNights = promotion.getMinimumNights();
        form.minimumAmount = promotion.getMinimumAmount();

        return form;
    }

    /**
     * Normalizes the promotion code before submitting the form.
     */
    public void normalize() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }

        if (description != null) {
            description = description.trim();

            if (description.isBlank()) {
                description = null;
            }
        }
    }

    // Getters and Setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PromotionType getType() {
        return type;
    }

    public void setType(PromotionType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getMinimumNights() {
        return minimumNights;
    }

    public void setMinimumNights(int minimumNights) {
        this.minimumNights = minimumNights;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }
}