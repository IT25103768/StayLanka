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
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{3,40}$")
    private String code;
    @NotBlank @Size(max = 160)
    private String name;
    @Size(max = 2000)
    private String description;
    @NotNull
    private PromotionType type;
    @NotNull @DecimalMin("0.01")
    private BigDecimal value;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    @Min(1)
    private int minimumNights = 1;
    @NotNull @DecimalMin("0.00")
    private BigDecimal minimumAmount = BigDecimal.ZERO;

    public static PromotionForm from(Promotion promotion) {
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

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PromotionType getType() { return type; }
    public void setType(PromotionType type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getMinimumNights() { return minimumNights; }
    public void setMinimumNights(int minimumNights) { this.minimumNights = minimumNights; }
    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }
}
