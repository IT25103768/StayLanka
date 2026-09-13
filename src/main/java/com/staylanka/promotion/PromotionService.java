package com.staylanka.promotion;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final Map<PromotionType, DiscountStrategy> strategies = new EnumMap<>(PromotionType.class);

    public PromotionService(PromotionRepository promotionRepository, List<DiscountStrategy> discountStrategies) {
        this.promotionRepository = promotionRepository;
        discountStrategies.forEach(strategy -> strategies.put(strategy.supports(), strategy));
    }

    @Transactional(readOnly = true)
    public Page<Promotion> active(int page) {
        LocalDate today = LocalDate.now();
        return promotionRepository.findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                today, today, PageRequest.of(Math.max(page, 0), 12, Sort.by("endDate").ascending()));
    }

    @Transactional(readOnly = true)
    public List<Promotion> all() {
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Promotion get(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion was not found."));
    }

    @Transactional
    public Promotion create(PromotionForm form) {
        validate(form);
        if (promotionRepository.existsByCodeIgnoreCase(form.getCode())) {
            throw new ConflictException("This promotion code is already in use.");
        }
        return promotionRepository.save(new Promotion(form.getCode().trim().toUpperCase(), form.getName().trim(),
                trimToNull(form.getDescription()), form.getType(), form.getValue(), form.getStartDate(),
                form.getEndDate(), form.getMinimumNights(), form.getMinimumAmount()));
    }

    @Transactional
    public void update(Long id, PromotionForm form) {
        validate(form);
        Promotion promotion = get(id);
        if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(form.getCode(), id)) {
            throw new ConflictException("This promotion code is already in use.");
        }
        promotion.update(form.getCode().trim().toUpperCase(), form.getName().trim(), trimToNull(form.getDescription()),
                form.getType(), form.getValue(), form.getStartDate(), form.getEndDate(),
                form.getMinimumNights(), form.getMinimumAmount());
    }

    @Transactional
    public void toggle(Long id) {
        Promotion promotion = get(id);
        promotion.setActive(!promotion.isActive());
    }

    @Transactional(readOnly = true)
    public PromotionResult apply(String code, BigDecimal grossAmount, long nights) {
        if (code == null || code.isBlank()) {
            return new PromotionResult(null, BigDecimal.ZERO);
        }
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessRuleException("Promotion code is invalid."));
        LocalDate today = LocalDate.now();
        if (!promotion.isActive() || today.isBefore(promotion.getStartDate()) || today.isAfter(promotion.getEndDate())) {
            throw new BusinessRuleException("Promotion is inactive or outside its valid dates.");
        }
        if (nights < promotion.getMinimumNights()) {
            throw new BusinessRuleException("This promotion requires at least " + promotion.getMinimumNights() + " nights.");
        }
        if (grossAmount.compareTo(promotion.getMinimumAmount()) < 0) {
            throw new BusinessRuleException("Reservation amount does not meet this promotion's minimum.");
        }
        DiscountStrategy strategy = strategies.get(promotion.getType());
        if (strategy == null) {
            throw new BusinessRuleException("Promotion calculation is unavailable.");
        }
        BigDecimal discount = strategy.calculate(grossAmount, promotion.getValue())
                .max(BigDecimal.ZERO).min(grossAmount);
        return new PromotionResult(promotion, discount);
    }

    private void validate(PromotionForm form) {
        if (form.getEndDate() != null && form.getStartDate() != null && form.getEndDate().isBefore(form.getStartDate())) {
            throw new BusinessRuleException("Promotion end date cannot be before the start date.");
        }
        if (form.getType() == PromotionType.PERCENTAGE && form.getValue() != null
                && form.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("A percentage discount cannot exceed 100%.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PromotionResult(Promotion promotion, BigDecimal discount) {
    }
}

