```java
        package com.staylanka.promotion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public PromotionType supports() {
        return PromotionType.PERCENTAGE;
    }

    @Override
    public BigDecimal calculate(BigDecimal grossAmount, BigDecimal promotionValue) {

        // Validate input values
        if (grossAmount == null) {
            throw new IllegalArgumentException("Gross amount must not be null");
        }

        if (promotionValue == null) {
            throw new IllegalArgumentException("Promotion value must not be null");
        }

        // Gross amount cannot be negative
        if (grossAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Gross amount cannot be negative"
            );
        }

        // Percentage must be between 0% and 100%
        if (promotionValue.compareTo(BigDecimal.ZERO) < 0
                || promotionValue.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Percentage discount must be between 0 and 100"
            );
        }

        // Calculate percentage discount
        return grossAmount
                .multiply(promotionValue)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
```

        ### Example

If:

        ```text
        grossAmount = 10,000
promotionValue = 15
        ```

the calculation is:

        ```text
10,000 × 15 ÷ 100
        = 1,500.00
        ```

So the method returns:

        ```text
1500.00
        ```

If your `DiscountStrategy` is supposed to return the **final amount after discount** instead, tell me and I can give you the full improved version for that design too.
