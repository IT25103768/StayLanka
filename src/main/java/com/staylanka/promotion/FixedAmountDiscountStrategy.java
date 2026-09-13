package com.staylanka.promotion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FixedAmountDiscountStrategy implements DiscountStrategy {
    @Override
    public PromotionType supports() {
        return PromotionType.FIXED_AMOUNT;
    }

    @Override
    public BigDecimal calculate(BigDecimal grossAmount, BigDecimal promotionValue) {
        return promotionValue;
    }
}

