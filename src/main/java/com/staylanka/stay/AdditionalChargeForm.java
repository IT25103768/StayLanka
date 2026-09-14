```java
        package com.staylanka.stay;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AdditionalChargeForm {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    @NotNull(message = "Unit price is required")
    @DecimalMin(
            value = "0.01",
            message = "Unit price must be greater than 0"
    )
    private BigDecimal unitPrice;

    public AdditionalChargeForm() {
    }

    /**
     * Creates a form object from an existing additional charge.
     */
    public static AdditionalChargeForm from(AdditionalCharge charge) {
        if (charge == null) {
            throw new IllegalArgumentException(
                    "Additional charge cannot be null"
            );
        }

        AdditionalChargeForm form = new AdditionalChargeForm();
        form.description = charge.getDescription();
        form.quantity = charge.getQuantity();
        form.unitPrice = charge.getUnitPrice() != null
                ? charge.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                : null;

        return form;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null
                ? description.trim()
                : null;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null
                ? unitPrice.setScale(2, RoundingMode.HALF_UP)
                : null;
    }
}
```
