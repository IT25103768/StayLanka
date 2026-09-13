package com.staylanka.stay;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AdditionalChargeForm {
    @NotBlank @Size(max = 200)
    private String description;
    @Min(1)
    private int quantity = 1;
    @NotNull @DecimalMin("0.01")
    private BigDecimal unitPrice;

    public static AdditionalChargeForm from(AdditionalCharge charge) {
        AdditionalChargeForm form = new AdditionalChargeForm();
        form.description = charge.getDescription();
        form.quantity = charge.getQuantity();
        form.unitPrice = charge.getUnitPrice();
        return form;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
