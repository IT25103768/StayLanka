```java
        package com.staylanka.promotion;

import java.math.BigDecimal;

/**
 * Strategy interface for calculating promotion discounts.
 *
 * Each implementation is responsible for handling
 * a specific type of promotion.
 */
public interface DiscountStrategy {

    /**
     * Returns the promotion type supported by this strategy.
     *
     * @return the supported promotion type
     */
    PromotionType supports();

    /**
     * Calculates the discount amount.
     *
     * @param grossAmount the original gross amount
     * @param promotionValue the value of the promotion
     * @return the calculated discount amount
     * @throws IllegalArgumentException if the input values are invalid
     */
    BigDecimal calculate(
            BigDecimal grossAmount,
            BigDecimal promotionValue
    );
}
```

        ### Together with your improved implementation

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
    public BigDecimal calculate(
            BigDecimal grossAmount,
            BigDecimal promotionValue
    ) {

        if (grossAmount == null) {
            throw new IllegalArgumentException(
                    "Gross amount must not be null"
            );
        }

        if (promotionValue == null) {
            throw new IllegalArgumentException(
                    "Promotion value must not be null"
            );
        }

        if (grossAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Gross amount cannot be negative"
            );
        }

        if (promotionValue.compareTo(BigDecimal.ZERO) < 0
                || promotionValue.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Percentage discount must be between 0 and 100"
            );
        }

        return grossAmount
                .multiply(promotionValue)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
```

This gives you a clean **Strategy Pattern** structure:

        ```text
        DiscountStrategy
       │
               └── PercentageDiscountStrategy
                    │
                            └── PERCENTAGE
```

The interface defines **what** a discount strategy must do, while `PercentageDiscountStrategy` defines **how** a percentage discount is calculated.
