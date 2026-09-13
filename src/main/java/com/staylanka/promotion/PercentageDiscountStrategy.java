package com.staylanka.promotion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {
    @Override
    public PromotionType supports() {
        return PromotionType.PERCENTAGE;
    }

    @Override
    public BigDecimal calculate(BigDecimal grossAmount, BigDecimal promotionValue) {
        return grossAmount.multiply(promotionValue)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

