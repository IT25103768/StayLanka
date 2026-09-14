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

    private static final int PAGE_SIZE = 12;

    private final PromotionRepository promotionRepository;
    private final Map<PromotionType, DiscountStrategy> strategies =
            new EnumMap<>(PromotionType.class);

    public PromotionService(
            PromotionRepository promotionRepository,
            List<DiscountStrategy> discountStrategies
    ) {
        this.promotionRepository = promotionRepository;

        discountStrategies.forEach(strategy -> {
            if (strategy == null || strategy.supports() == null) {
                throw new IllegalArgumentException(
                        "Discount strategy and supported promotion type cannot be null."
                );
            }

            DiscountStrategy existing =
                    strategies.put(strategy.supports(), strategy);

            if (existing != null) {
                throw new IllegalStateException(
                        "Multiple discount strategies found for promotion type: "
                                + strategy.supports()
                );
            }
        });
    }

    /**
     * Returns active promotions that are valid today.
     */
    @Transactional(readOnly = true)
    public Page<Promotion> active(int page) {
        int safePage = Math.max(page, 0);

        LocalDate today = LocalDate.now();

        PageRequest pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "endDate")
        );

        return promotionRepository
                .findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        today,
                        today,
                        pageable
                );
    }

    /**
     * Returns all promotions ordered by newest first.
     */
    @Transactional(readOnly = true)
    public List<Promotion> all() {
        return promotionRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    /**
     * Finds a promotion by ID.
     */
    @Transactional(readOnly = true)
    public Promotion get(Long id) {
        validateId(id);

        return promotionRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Promotion was not found.")
                );
    }

    /**
     * Creates a new promotion.
     */
    @Transactional
    public Promotion create(PromotionForm form) {
        validateForm(form);

        String code = normalizeCode(form.getCode());

        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(
                    "This promotion code is already in use."
            );
        }

        Promotion promotion = new Promotion(
                code,
                normalizeRequiredText(form.getName(), "Promotion name"),
                trimToNull(form.getDescription()),
                form.getType(),
                form.getValue(),
                form.getStartDate(),
                form.getEndDate(),
                form.getMinimumNights(),
                form.getMinimumAmount()
        );

        return promotionRepository.save(promotion);
    }

    /**
     * Updates an existing promotion.
     */
    @Transactional
    public void update(Long id, PromotionForm form) {
        validateId(id);
        validateForm(form);

        Promotion promotion = get(id);

        String code = normalizeCode(form.getCode());

        if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException(
                    "This promotion code is already in use."
            );
        }

        promotion.update(
                code,
                normalizeRequiredText(form.getName(), "Promotion name"),
                trimToNull(form.getDescription()),
                form.getType(),
                form.getValue(),
                form.getStartDate(),
                form.getEndDate(),
                form.getMinimumNights(),
                form.getMinimumAmount()
        );
    }

    /**
     * Activates or deactivates a promotion.
     */
    @Transactional
    public void toggle(Long id) {
        Promotion promotion = get(id);

        if (promotion.isActive()) {
            promotion.deactivate();
        } else {
            promotion.activate();
        }
    }

    /**
     * Applies a promotion code to a reservation.
     */
    @Transactional(readOnly = true)
    public PromotionResult apply(
            String code,
            BigDecimal grossAmount,
            long nights
    ) {
        if (code == null || code.isBlank()) {
            return new PromotionResult(null, BigDecimal.ZERO);
        }

        if (grossAmount == null) {
            throw new BusinessRuleException(
                    "Reservation amount cannot be null."
            );
        }

        if (grossAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "Reservation amount cannot be negative."
            );
        }

        if (nights < 0) {
            throw new BusinessRuleException(
                    "Number of nights cannot be negative."
            );
        }

        String normalizedCode = normalizeCode(code);

        Promotion promotion = promotionRepository
                .findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Promotion code is invalid."
                        )
                );

        validatePromotionEligibility(
                promotion,
                grossAmount,
                nights
        );

        DiscountStrategy strategy = strategies.get(promotion.getType());

        if (strategy == null) {
            throw new BusinessRuleException(
                    "Promotion calculation is unavailable."
            );
        }

        BigDecimal discount = strategy
                .calculate(grossAmount, promotion.getValue())
                .max(BigDecimal.ZERO)
                .min(grossAmount);

        return new PromotionResult(
                promotion,
                discount
        );
    }

    /**
     * Validates whether a promotion can be applied.
     */
    private void validatePromotionEligibility(
            Promotion promotion,
            BigDecimal grossAmount,
            long nights
    ) {
        LocalDate today = LocalDate.now();

        if (!promotion.isValidOn(today)) {
            throw new BusinessRuleException(
                    "Promotion is inactive or outside its valid dates."
            );
        }

        if (!promotion.meetsMinimumNights((int) nights)) {
            throw new BusinessRuleException(
                    "This promotion requires at least "
                            + promotion.getMinimumNights()
                            + " nights."
            );
        }

        if (!promotion.meetsMinimumAmount(grossAmount)) {
            throw new BusinessRuleException(
                    "Reservation amount does not meet this promotion's minimum."
            );
        }
    }

    /**
     * Validates promotion form business rules.
     */
    private void validateForm(PromotionForm form) {
        if (form == null) {
            throw new BusinessRuleException(
                    "Promotion details are required."
            );
        }

        LocalDate startDate = form.getStartDate();
        LocalDate endDate = form.getEndDate();

        if (startDate != null
                && endDate != null
                && endDate.isBefore(startDate)) {

            throw new BusinessRuleException(
                    "Promotion end date cannot be before the start date."
            );
        }

        if (form.getType() == PromotionType.PERCENTAGE
                && form.getValue() != null
                && form.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new BusinessRuleException(
                    "A percentage discount cannot exceed 100%."
            );
        }
    }

    /**
     * Validates an entity ID.
     */
    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Promotion ID must be a positive number."
            );
        }
    }

    /**
     * Normalizes promotion codes consistently.
     */
    private String normalizeCode(String code) {
        if (code == null) {
            throw new BusinessRuleException(
                    "Promotion code is required."
            );
        }

        return code.trim().toUpperCase();
    }

    /**
     * Trims optional text and converts blank values to null.
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Validates and normalizes required text.
     */
    private String normalizeRequiredText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }

    public record PromotionResult(
            Promotion promotion,
            BigDecimal discount
    ) {
    }
}