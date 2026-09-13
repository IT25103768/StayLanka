package com.staylanka.promotion;

import java.math.BigDecimal;

public interface DiscountStrategy {
    PromotionType supports();
    BigDecimal calculate(BigDecimal grossAmount, BigDecimal promotionValue);
}

