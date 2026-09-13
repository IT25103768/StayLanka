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
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final StayService stayService;
    private final CurrentUserService currentUserService;

    public ReviewService(ReviewRepository reviewRepository, StayService stayService,
                         CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.stayService = stayService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<Review> approved(int page) {
        return reviewRepository.findByStatus(ReviewStatus.APPROVED,
                PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<Review> own(Authentication authentication, int page) {
        return reviewRepository.findByCustomerUserEmailIgnoreCase(authentication.getName(),
                PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<Review> moderate(ReviewStatus status, int page) {
        return reviewRepository.findByStatus(status == null ? ReviewStatus.PENDING : status,
                PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public Review create(Authentication authentication, Long stayId, ReviewForm form) {
        CustomerProfile customer = currentUserService.customer(authentication);
        Stay stay = stayService.own(authentication, stayId);
        validateEligibility(stay);
        if (reviewRepository.existsByStayId(stayId)) {
            throw new ConflictException("A review has already been submitted for this stay.");
        }
        return reviewRepository.save(new Review(stay, customer, form.getRating(), form.getComment().trim()));
    }

    @Transactional(readOnly = true)
    public Review ownReview(Authentication authentication, Long id) {
        Review review = detailed(id);
        if (!review.getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new NotFoundException("Review was not found.");
        }
        return review;
    }

    @Transactional
    public void update(Authentication authentication, Long id, ReviewForm form) {
        ownReview(authentication, id).update(form.getRating(), form.getComment().trim());
    }

    @Transactional
    public void delete(Authentication authentication, Long id) {
        reviewRepository.delete(ownReview(authentication, id));
    }

    @Transactional
    public void approve(Long id) {
        detailed(id).moderate(ReviewStatus.APPROVED);
    }

    @Transactional
    public void reject(Long id) {
        detailed(id).moderate(ReviewStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public Review detailed(Long id) {
        return reviewRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Review was not found."));
    }

    @Transactional(readOnly = true)
    public boolean canReview(Authentication authentication, Long stayId) {
        try {
            Stay stay = stayService.own(authentication, stayId);
            return stay.isCompleted() && !reviewRepository.existsByStayId(stayId);
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    private void validateEligibility(Stay stay) {
        if (!stay.isCompleted()) {
            throw new BusinessRuleException("Only a completed stay can be reviewed.");
        }
    }
}
