package com.staylanka.promotion;

import com.staylanka.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    protected Promotion() {
    }

    public Promotion(String code, String name, String description, PromotionType type, BigDecimal value,
                     LocalDate startDate, LocalDate endDate, int minimumNights, BigDecimal minimumAmount) {
        update(code, name, description, type, value, startDate, endDate, minimumNights, minimumAmount);
    }

    public void update(String code, String name, String description, PromotionType type, BigDecimal value,
                       LocalDate startDate, LocalDate endDate, int minimumNights, BigDecimal minimumAmount) {
        this.code = code.toUpperCase();
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = value;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minimumNights = minimumNights;
        this.minimumAmount = minimumAmount;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public PromotionType getType() { return type; }
    public BigDecimal getValue() { return value; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getMinimumNights() { return minimumNights; }
    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
