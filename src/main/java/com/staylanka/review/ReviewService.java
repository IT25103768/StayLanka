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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.staylanka.common.AuditService audit;

    @org.springframework.beans.factory.annotation.Autowired
    private com.staylanka.common.InputRules rules;

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

    @Transactional(readOnly = true)
    public Page<Review> approved(int page) {
        return reviewRepository.findByStatus(
                ReviewStatus.APPROVED,
                PageRequest.of(
                        Math.max(page, 0),
                        12,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<Review> own(Authentication authentication, int page) {
        return reviewRepository.findByCustomerUserEmailIgnoreCase(
                authentication.getName(),
                PageRequest.of(
                        Math.max(page, 0),
                        12,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<Review> moderate(ReviewStatus status, int page) {
        ReviewStatus selected = status == null ? ReviewStatus.PENDING : status;

        return reviewRepository.findByStatus(
                selected,
                PageRequest.of(
                        Math.max(page, 0),
                        15,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }

    /**
     * Customer creates one review for one completed stay that belongs to them.
     * New reviews always begin in PENDING state.
     */
    @Transactional
    public Review create(
            Authentication authentication,
            Long stayId,
            ReviewForm form
    ) {
        rules.validate(form);

        CustomerProfile customer = currentUserService.customer(authentication);
        Stay stay = stayService.own(authentication, stayId);

        validateEligibility(stay);

        if (reviewRepository.existsByStayId(stayId)) {
            throw new ConflictException(
                    "A review has already been submitted for this stay."
            );
        }

        Review created = reviewRepository.save(
                new Review(
                        stay,
                        customer,
                        form.getRating(),
                        form.getComment().trim()
                )
        );

        audit.record(created, "CREATE");
        return created;
    }

    /**
     * Loads a review only when it belongs to the currently authenticated customer.
     */
    @Transactional(readOnly = true)
    public Review ownReview(Authentication authentication, Long id) {
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
     * Customer updates their own review. Review.update(...) resets the status
     * to PENDING so an edited review must be moderated again.
     */
    @Transactional
    public void update(
            Authentication authentication,
            Long id,
            ReviewForm form
    ) {
        rules.validate(form);

        Review review = ownReview(authentication, id);
        validateEligibility(review.getStay());

        review.update(
                form.getRating(),
                form.getComment().trim()
        );

        audit.record(review, "UPDATE");
    }

    /**
     * Kept for compatibility with the existing lifecycle integration test.
     * This is the old soft-hide behavior and is no longer used by the customer UI.
     */
    @Transactional
    public void delete(Authentication authentication, Long id) {
        Review review = ownReview(authentication, id);
        review.moderate(ReviewStatus.REJECTED);
        audit.record(review, "HIDE");
    }

    /**
     * Customer permanently deletes their own review from the reviews table.
     */
    @Transactional
    public void deleteOwnPermanently(
            Authentication authentication,
            Long id
    ) {
        Review review = ownReview(authentication, id);
        Long reviewId = review.getId();

        audit.record(
                "Review",
                reviewId,
                "HARD_DELETE",
                "Customer permanently deleted own review"
        );

        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    @Transactional
    public void approve(Long id) {
        Review review = detailed(id);
        validateEligibility(review.getStay());
        review.moderate(ReviewStatus.APPROVED);
        audit.record(review, "APPROVE");
    }

    @Transactional
    public void reject(Long id) {
        Review review = detailed(id);
        review.moderate(ReviewStatus.REJECTED);
        audit.record(review, "REJECT");
    }

    /**
     * Promotion & Review Manager / Admin permanently deletes any review.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('PROMOTION_REVIEW_MANAGER','ADMIN')")
    public void deletePermanently(Long id) {
        Review review = detailed(id);
        Long reviewId = review.getId();

        audit.record(
                "Review",
                reviewId,
                "HARD_DELETE",
                "Review permanently deleted by authorised review manager"
        );

        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    @Transactional(readOnly = true)
    public Review detailed(Long id) {
        return reviewRepository.findDetailedById(id)
                .orElseThrow(
                        () -> new NotFoundException("Review was not found.")
                );
    }

    @Transactional(readOnly = true)
    public boolean canReview(
            Authentication authentication,
            Long stayId
    ) {
        try {
            Stay stay = stayService.own(authentication, stayId);

            return stay.isCompleted()
                    && !reviewRepository.existsByStayId(stayId);

        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean hasReviewForStay(Long stayId) {
        return reviewRepository.existsByStayId(stayId);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    private void validateEligibility(Stay stay) {
        if (!stay.isCompleted()) {
            throw new BusinessRuleException(
                    "Only a completed stay can be reviewed."
            );
        }
    }
}
