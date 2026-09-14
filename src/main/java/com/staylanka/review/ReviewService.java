package com.staylanka.review;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.security.CurrentUserService;
import com.staylanka.stay.Stay;
import com.staylanka.stay.StayService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private static final int APPROVED_PAGE_SIZE = 12;
    private static final int MODERATION_PAGE_SIZE = 15;

    private static final String CREATED_AT = "createdAt";

    private final ReviewRepository reviewRepository;
    private final StayService stayService;
    private final CurrentUserService currentUserService;

    public ReviewService(
            ReviewRepository reviewRepository,
            StayService stayService,
            CurrentUserService currentUserService
    ) {
        this.reviewRepository = reviewRepository;
        this.stayService = stayService;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns approved reviews using pagination.
     */
    public Page<Review> approved(int page) {
        return reviewRepository.findByStatus(
                ReviewStatus.APPROVED,
                pageRequest(page, APPROVED_PAGE_SIZE)
        );
    }

    /**
     * Returns reviews created by the currently authenticated customer.
     */
    public Page<Review> own(Authentication authentication, int page) {
        requireAuthentication(authentication);

        return reviewRepository.findByCustomerUserEmailIgnoreCase(
                authentication.getName(),
                pageRequest(page, APPROVED_PAGE_SIZE)
        );
    }

    /**
     * Returns reviews for moderation.
     *
     * If no status is supplied, pending reviews are returned.
     */
    public Page<Review> moderate(ReviewStatus status, int page) {
        ReviewStatus reviewStatus =
                status == null ? ReviewStatus.PENDING : status;

        return reviewRepository.findByStatus(
                reviewStatus,
                pageRequest(page, MODERATION_PAGE_SIZE)
        );
    }

    /**
     * Creates a new review for a completed stay.
     */
    @Transactional
    public Review create(
            Authentication authentication,
            Long stayId,
            ReviewForm form
    ) {
        requireAuthentication(authentication);
        requireValidStayId(stayId);
        requireValidForm(form);

        CustomerProfile customer = currentUserService.customer(authentication);
        Stay stay = stayService.own(authentication, stayId);

        validateEligibility(stay);
        validateReviewDoesNotExist(stayId);

        return reviewRepository.save(
                new Review(
                        stay,
                        customer,
                        form.getRating(),
                        form.getComment().trim()
                )
        );
    }

    /**
     * Returns a review only if it belongs to the authenticated user.
     */
    public Review ownReview(Authentication authentication, Long id) {
        requireAuthentication(authentication);

        Review review = detailed(id);

        if (!review.getCustomer()
                .getUser()
                .getEmail()
                .equalsIgnoreCase(authentication.getName())) {

            throw new NotFoundException("Review was not found.");
        }

        return review;
    }

    /**
     * Updates the authenticated user's review.
     */
    @Transactional
    public void update(
            Authentication authentication,
            Long id,
            ReviewForm form
    ) {
        requireValidForm(form);

        Review review = ownReview(authentication, id);

        review.update(
                form.getRating(),
                form.getComment().trim()
        );
    }

    /**
     * Deletes the authenticated user's review.
     */
    @Transactional
    public void delete(Authentication authentication, Long id) {
        Review review = ownReview(authentication, id);

        reviewRepository.delete(review);
    }

    /**
     * Approves a review.
     */
    @Transactional
    public void approve(Long id) {
        Review review = detailed(id);

        validateModeration(review);

        review.moderate(ReviewStatus.APPROVED);
    }

    /**
     * Rejects a review.
     */
    @Transactional
    public void reject(Long id) {
        Review review = detailed(id);

        validateModeration(review);

        review.moderate(ReviewStatus.REJECTED);
    }

    /**
     * Returns a review by ID.
     */
    public Review detailed(Long id) {
        requireValidReviewId(id);

        return reviewRepository.findDetailedById(id)
                .orElseThrow(() ->
                        new NotFoundException("Review was not found.")
                );
    }

    /**
     * Checks whether the customer can submit a review for a stay.
     */
    public boolean canReview(
            Authentication authentication,
            Long stayId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (stayId == null || stayId <= 0) {
            return false;
        }

        try {
            Stay stay = stayService.own(authentication, stayId);

            return stay.isCompleted()
                    && !reviewRepository.existsByStayId(stayId);

        } catch (NotFoundException ex) {
            return false;
        }
    }

    /**
     * Returns the number of pending reviews.
     */
    public long pendingCount() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    /**
     * Validates whether a stay is eligible for reviewing.
     */
    private void validateEligibility(Stay stay) {
        if (stay == null) {
            throw new NotFoundException("Stay was not found.");
        }

        if (!stay.isCompleted()) {
            throw new BusinessRuleException(
                    "Only a completed stay can be reviewed."
            );
        }
    }

    /**
     * Prevents multiple reviews for the same stay.
     */
    private void validateReviewDoesNotExist(Long stayId) {
        if (reviewRepository.existsByStayId(stayId)) {
            throw new ConflictException(
                    "A review has already been submitted for this stay."
            );
        }
    }

    /**
     * Prevents unnecessary moderation state changes.
     */
    private void validateModeration(Review review) {
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only pending reviews can be moderated."
            );
        }
    }

    /**
     * Creates a consistent pagination request.
     */
    private PageRequest pageRequest(int page, int pageSize) {
        return PageRequest.of(
                Math.max(page, 0),
                pageSize,
                Sort.by(
                        Sort.Direction.DESC,
                        CREATED_AT
                )
        );
    }

    /**
     * Validates authentication.
     */
    private void requireAuthentication(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new BusinessRuleException(
                    "Authentication is required."
            );
        }
    }

    /**
     * Validates review form.
     */
    private void requireValidForm(ReviewForm form) {
        if (form == null) {
            throw new BusinessRuleException(
                    "Review form is required."
            );
        }

        if (form.getComment() == null
                || form.getComment().trim().isBlank()) {

            throw new BusinessRuleException(
                    "Review comment cannot be empty."
            );
        }
    }

    /**
     * Validates stay ID.
     */
    private void requireValidStayId(Long stayId) {
        if (stayId == null || stayId <= 0) {
            throw new BusinessRuleException(
                    "Invalid stay ID."
            );
        }
    }

    /**
     * Validates review ID.
     */
    private void requireValidReviewId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(
                    "Invalid review ID."
            );
        }
    }
}