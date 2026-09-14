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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PromotionService {

    private static final int PAGE_SIZE = 12;
    private static final int MONEY_SCALE = 2;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final Map<PromotionType, DiscountStrategy> strategies =
            new EnumMap<>(PromotionType.class);

    public PromotionService(
            PromotionRepository promotionRepository,
            List<DiscountStrategy> discountStrategies
    ) {
        this.promotionRepository = promotionRepository;

        registerStrategies(discountStrategies);
    }

    /**
     * Returns active promotions that are valid today.
     */
    public Page<Promotion> active(int page) {
        LocalDate today = LocalDate.now();

        return promotionRepository
                .findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        today,
                        today,
                        pageRequest(page)
                );
    }

    /**
     * Returns all promotions ordered by newest first.
     */
    public List<Promotion> all() {
        return promotionRepository.findAll(
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }

    /**
     * Finds a promotion by ID.
     */
    public Promotion get(Long id) {
        validateId(id);

        return promotionRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Promotion was not found."
                        )
                );
    }

    /**
     * Creates a new promotion.
     */
    @Transactional
    public Promotion create(PromotionForm form) {
        validateForm(form);

        String code = normalizeCode(form.getCode());

        ensureCodeAvailable(code);

        Promotion promotion = new Promotion(
                code,
                normalizeRequiredText(
                        form.getName(),
                        "Promotion name"
                ),
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

        ensureCodeAvailableForUpdate(code, id);

        promotion.update(
                code,
                normalizeRequiredText(
                        form.getName(),
                        "Promotion name"
                ),
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
    public PromotionResult apply(
            String code,
            BigDecimal grossAmount,
            long nights
    ) {
        if (code == null || code.isBlank()) {
            return emptyResult();
        }

        validateGrossAmount(grossAmount);
        validateNights(nights);

        String normalizedCode = normalizeCode(code);

        Promotion promotion = findPromotionByCode(normalizedCode);

        validatePromotionEligibility(
                promotion,
                grossAmount,
                nights
        );

        DiscountStrategy strategy = getStrategy(promotion.getType());

        BigDecimal discount = calculateDiscount(
                strategy,
                grossAmount,
                promotion.getValue()
        );

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

        if (nights > Integer.MAX_VALUE) {
            throw new BusinessRuleException(
                    "Number of nights is too large."
            );
        }

        int numberOfNights = (int) nights;

        if (!promotion.meetsMinimumNights(numberOfNights)) {
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

        validateDateRange(
                form.getStartDate(),
                form.getEndDate()
        );

        validatePromotionType(form);

        validatePromotionValue(form);

        validateMinimumNights(form);

        validateMinimumAmount(form);
    }

    /**
     * Validates promotion start and end dates.
     */
    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null
                && endDate != null
                && endDate.isBefore(startDate)) {

            throw new BusinessRuleException(
                    "Promotion end date cannot be before the start date."
            );
        }
    }

    /**
     * Validates promotion type.
     */
    private void validatePromotionType(PromotionForm form) {
        if (form.getType() == null) {
            throw new BusinessRuleException(
                    "Promotion type is required."
            );
        }
    }

    /**
     * Validates promotion value.
     */
    private void validatePromotionValue(PromotionForm form) {
        BigDecimal value = form.getValue();

        if (value == null) {
            throw new BusinessRuleException(
                    "Promotion value is required."
            );
        }

        if (value.compareTo(ZERO) < 0) {
            throw new BusinessRuleException(
                    "Promotion value cannot be negative."
            );
        }

        if (form.getType() == PromotionType.PERCENTAGE
                && value.compareTo(ONE_HUNDRED) > 0) {

            throw new BusinessRuleException(
                    "A percentage discount cannot exceed 100%."
            );
        }
    }

    /**
     * Validates minimum nights.
     */
    private void validateMinimumNights(PromotionForm form) {
        Integer minimumNights = form.getMinimumNights();

        if (minimumNights != null && minimumNights < 0) {
            throw new BusinessRuleException(
                    "Minimum nights cannot be negative."
            );
        }
    }

    /**
     * Validates minimum reservation amount.
     */
    private void validateMinimumAmount(PromotionForm form) {
        BigDecimal minimumAmount = form.getMinimumAmount();

        if (minimumAmount != null
                && minimumAmount.compareTo(ZERO) < 0) {

            throw new BusinessRuleException(
                    "Minimum amount cannot be negative."
            );
        }
    }

    /**
     * Calculates and safely limits the discount.
     */
    private BigDecimal calculateDiscount(
            DiscountStrategy strategy,
            BigDecimal grossAmount,
            BigDecimal promotionValue
    ) {
        BigDecimal discount = strategy.calculate(
                grossAmount,
                promotionValue
        );

        if (discount == null) {
            throw new BusinessRuleException(
                    "Promotion calculation returned no discount."
            );
        }

        return discount
                .max(ZERO)
                .min(grossAmount)
                .setScale(
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    /**
     * Returns the strategy for a promotion type.
     */
    private DiscountStrategy getStrategy(PromotionType type) {
        DiscountStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new BusinessRuleException(
                    "Promotion calculation is unavailable."
            );
        }

        return strategy;
    }

    /**
     * Finds a promotion using its normalized code.
     */
    private Promotion findPromotionByCode(String code) {
        return promotionRepository
                .findByCodeIgnoreCase(code)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Promotion code is invalid."
                        )
                );
    }

    /**
     * Ensures a promotion code is not already used.
     */
    private void ensureCodeAvailable(String code) {
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(
                    "This promotion code is already in use."
            );
        }
    }

    /**
     * Ensures a promotion code is available during an update.
     */
    private void ensureCodeAvailableForUpdate(
            String code,
            Long id
    ) {
        if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(
                code,
                id
        )) {
            throw new ConflictException(
                    "This promotion code is already in use."
            );
        }
    }

    /**
     * Registers all discount strategies.
     */
    private void registerStrategies(
            List<DiscountStrategy> discountStrategies
    ) {
        if (discountStrategies == null) {
            throw new IllegalArgumentException(
                    "Discount strategies cannot be null."
            );
        }

        for (DiscountStrategy strategy : discountStrategies) {
            if (strategy == null) {
                throw new IllegalArgumentException(
                        "Discount strategy cannot be null."
                );
            }

            PromotionType type = strategy.supports();

            if (type == null) {
                throw new IllegalArgumentException(
                        "Discount strategy type cannot be null."
                );
            }

            DiscountStrategy existing = strategies.put(type, strategy);

            if (existing != null) {
                throw new IllegalStateException(
                        "Multiple discount strategies found for promotion type: "
                                + type
                );
            }
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
     * Validates the reservation amount.
     */
    private void validateGrossAmount(BigDecimal grossAmount) {
        if (grossAmount == null) {
            throw new BusinessRuleException(
                    "Reservation amount cannot be null."
            );
        }

        if (grossAmount.compareTo(ZERO) < 0) {
            throw new BusinessRuleException(
                    "Reservation amount cannot be negative."
            );
        }
    }

    /**
     * Validates the number of nights.
     */
    private void validateNights(long nights) {
        if (nights < 0) {
            throw new BusinessRuleException(
                    "Number of nights cannot be negative."
            );
        }
    }

    /**
     * Normalizes promotion codes consistently.
     */
    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
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

    /**
     * Creates a consistent pagination request.
     */
    private PageRequest pageRequest(int page) {
        return PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(
                        Sort.Direction.ASC,
                        "endDate"
                )
        );
    }

    /**
     * Returns an empty promotion result.
     */
    private PromotionResult emptyResult() {
        return new PromotionResult(
                null,
                ZERO.setScale(MONEY_SCALE)
        );
    }

    public record PromotionResult(
            Promotion promotion,
            BigDecimal discount
    ) {
    }
}